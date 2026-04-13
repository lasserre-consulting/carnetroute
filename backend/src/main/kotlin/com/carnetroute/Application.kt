package com.carnetroute

import com.carnetroute.application.usecase.*
import com.carnetroute.domain.model.FuelPriceAlert
import com.carnetroute.domain.model.LiveFuelPrices
import com.carnetroute.domain.port.GeocodingPort
import com.carnetroute.domain.port.RoutingPort
import com.carnetroute.domain.service.SimulationEngine
import com.carnetroute.infrastructure.geocoding.GouvGeocodingAdapter
import com.carnetroute.infrastructure.kafka.FuelPriceConsumer
import com.carnetroute.infrastructure.kafka.FuelPriceProducer
import com.carnetroute.infrastructure.routes.simulationRoutes
import com.carnetroute.infrastructure.routing.OsrmRoutingAdapter
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.util.Collections
import kotlin.time.Duration.Companion.seconds

private val logger = LoggerFactory.getLogger("carnetroute")

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    // ── Injection de dépendances manuelle ────────────────────────────────────
    val engine:        SimulationEngine = SimulationEngine()
    val routingPort:   RoutingPort      = OsrmRoutingAdapter()
    val geocodingPort: GeocodingPort    = GouvGeocodingAdapter()

    val useCases = SimulationUseCases(
        simulate      = SimulateRouteUseCase(routingPort, engine),
        heatmap       = GenerateHeatmapUseCase(routingPort, engine),
        fuels         = GetFuelProfilesUseCase(engine),
        geocode       = GeocodeUseCase(geocodingPort),
        reverseGeocode= ReverseGeocodeUseCase(geocodingPort)
    )

    // ── Kafka ─────────────────────────────────────────────────────────────────
    val priceProducer = FuelPriceProducer()
    val priceConsumer = FuelPriceConsumer()

    // ── WebSocket sessions ────────────────────────────────────────────────────
    val json       = Json { prettyPrint = true; isLenient = true; ignoreUnknownKeys = true; encodeDefaults = true }
    val wsSessions = Collections.synchronizedSet(mutableSetOf<WebSocketSession>())

    priceConsumer.onAlert = { alert: FuelPriceAlert ->
        val alertJson = json.encodeToString(alert)
        val dead = mutableListOf<WebSocketSession>()
        wsSessions.forEach { session ->
            try { launch { session.send(Frame.Text(alertJson)) } }
            catch (_: Exception) { dead.add(session) }
        }
        dead.forEach { wsSessions.remove(it) }
    }

    // ── Plugins ───────────────────────────────────────────────────────────────
    install(ContentNegotiation) { json(json) }

    install(WebSockets) {
        pingPeriod  = 15.seconds
        timeout     = 60.seconds
        maxFrameSize= 64 * 1024L
        masking     = true
    }

    install(CORS) {
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Options)
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Authorization)
        allowHost("localhost:4200")
        allowHost("localhost:80")
        allowHost("localhost")
        allowHost("www.lasserre-consulting.fr", schemes = listOf("https"))
    }

    install(CallLogging)

    install(StatusPages) {
        exception<Throwable> { call, cause ->
            logger.error("Exception non gérée", cause)
            call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Erreur interne du serveur"))
        }
    }

    // ── Routes ────────────────────────────────────────────────────────────────
    routing {
        simulationRoutes(useCases)

        get("/api/prices/live") {
            call.respond(LiveFuelPrices(
                prices     = priceProducer.getCurrentPrices(),
                lastUpdate = System.currentTimeMillis(),
                alerts     = priceConsumer.getRecentAlerts().take(10)
            ))
        }

        get("/api/alerts") {
            call.respond(priceConsumer.getRecentAlerts())
        }

        webSocket("/ws/alerts") {
            wsSessions.add(this)
            try {
                priceConsumer.getRecentAlerts().take(5).forEach { alert ->
                    send(Frame.Text(json.encodeToString(alert)))
                }
                for (frame in incoming) { /* keep-alive */ }
            } finally {
                wsSessions.remove(this)
            }
        }
    }

    // ── Lifecycle Kafka ───────────────────────────────────────────────────────
    val kafkaScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    environment.monitor.subscribe(ApplicationStarted) {
        logger.info("Démarrage du monitoring Kafka carburant...")
        priceProducer.start(kafkaScope)
        priceConsumer.start(kafkaScope)
    }

    environment.monitor.subscribe(ApplicationStopped) {
        logger.info("Arrêt du monitoring Kafka carburant...")
        priceProducer.stop()
        priceConsumer.stop()
        kafkaScope.cancel()
    }
}
