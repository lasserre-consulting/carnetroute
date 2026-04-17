package com.carnetroute.infrastructure.routing

import com.carnetroute.domain.simulation.vo.Coordinates
import com.carnetroute.domain.simulation.vo.Route
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.slf4j.LoggerFactory

class OsrmRoutingAdapter : RoutingPort {

    private val logger = LoggerFactory.getLogger(OsrmRoutingAdapter::class.java)

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 10_000
            connectTimeoutMillis = 5_000
        }
    }

    override suspend fun getRoute(from: Coordinates, to: Coordinates): Route {
        return try {
            val url = buildUrl(from, to)
            val response = client.get(url)

            if (!response.status.isSuccess()) {
                logger.warn("OSRM returned HTTP ${response.status.value}, falling back to Haversine")
                return HaversineCalculator.getRoute(from, to)
            }

            val body = response.bodyAsText()
            parseOsrmResponse(body, from, to)
        } catch (e: Exception) {
            logger.warn("OSRM request failed (${e.message}), falling back to Haversine")
            HaversineCalculator.getRoute(from, to)
        }
    }

    private fun buildUrl(from: Coordinates, to: Coordinates): String =
        "https://router.project-osrm.org/route/v1/driving/" +
            "${from.lng},${from.lat};${to.lng},${to.lat}" +
            "?overview=full&geometries=geojson"

    private fun parseOsrmResponse(body: String, from: Coordinates, to: Coordinates): Route {
        val root = Json.parseToJsonElement(body).jsonObject
        val route = root["routes"]
            ?.jsonArray
            ?.firstOrNull()
            ?.jsonObject
            ?: throw IllegalStateException("No routes in OSRM response")

        val distanceKm = route["distance"]!!.jsonPrimitive.double / 1000.0
        val durationMinutes = route["duration"]!!.jsonPrimitive.double / 60.0

        val geometry: List<List<Double>> = route["geometry"]
            ?.jsonObject
            ?.get("coordinates")
            ?.jsonArray
            ?.map { coord ->
                coord.jsonArray.map { it.jsonPrimitive.double }
            }
            ?: emptyList()

        return Route(
            from = from,
            to = to,
            distanceKm = distanceKm,
            durationMinutes = durationMinutes,
            geometry = geometry
        )
    }
}
