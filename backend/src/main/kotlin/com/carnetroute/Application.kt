package com.carnetroute

import com.carnetroute.kafka.FuelPriceConsumer
import com.carnetroute.kafka.FuelPriceProducer
import com.carnetroute.routes.simulationRoutes
import com.carnetroute.services.GeocodingService
import com.carnetroute.services.SimulationService
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
import kotlin.time.Duration.Companion.seconds
import java.util.Collections

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    val simulationService = SimulationService()
    val geocodingService = GeocodingService()
    val json = Json { prettyPrint = true; isLenient = true; ignoreUnknownKeys = true; encodeDefaults = true }

    // Kafka
    val priceProducer = FuelPriceProducer()
    val priceConsumer = FuelPriceConsumer()

    // WebSocket sessions
    val wsSessions = Collections.synchronizedSet(mutableSetOf<WebSocketSession>())

    // Broadcast alerts to all WebSocket clients
    priceConsumer.onAlert = { alert ->
        val alertJson = json.encodeToString(alert)
        val dead = mutableListOf<WebSocketSession>()
        wsSessions.forEach { session ->
            try {
                launch { session.send(Frame.Text(alertJson)) }
            } catch (_: Exception) { dead.add(session) }
        }
        dead.forEach { wsSessions.remove(it) }
    }

    install(ContentNegotiation) { json(json) }

    install(WebSockets) {
        pingPeriod = 15.seconds
        timeout = 60.seconds
        maxFrameSize = 64 * 1024L
        masking = true
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
    }

    install(CallLogging)

    install(StatusPages) {
        exception<Throwable> { call, cause ->
            log.error("Unhandled exception", cause)
            call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Internal server error"))
        }
    }

    routing {
        simulationRoutes(simulationService, geocodingService)

        // Live fuel prices (updated by Kafka producer)
        get("/api/prices/live") {
            call.respond(com.carnetroute.models.LiveFuelPrices(
                prices = priceProducer.getCurrentPrices(),
                lastUpdate = System.currentTimeMillis(),
                alerts = priceConsumer.getRecentAlerts().take(10)
            ))
        }

        // Alert history
        get("/api/alerts") {
            call.respond(priceConsumer.getRecentAlerts())
        }

        // WebSocket: real-time fuel price alerts
        webSocket("/ws/alerts") {
            wsSessions.add(this)
            try {
                priceConsumer.getRecentAlerts().take(5).forEach { alert ->
                    send(Frame.Text(json.encodeToString(alert)))
                }
                for (frame in incoming) { /* keep alive */ }
            } finally {
                wsSessions.remove(this)
            }
        }
    }

    // Kafka lifecycle
    val kafkaScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    environment.monitor.subscribe(ApplicationStarted) {
        log.info("Starting Kafka fuel price monitoring...")
        priceProducer.start(kafkaScope)
        priceConsumer.start(kafkaScope)
    }

    environment.monitor.subscribe(ApplicationStopped) {
        log.info("Stopping Kafka fuel price monitoring...")
        priceProducer.stop()
        priceConsumer.stop()
        kafkaScope.cancel()
    }
}
