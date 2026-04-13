package com.carnetroute.infrastructure.routing

import com.carnetroute.domain.model.Coordinates
import com.carnetroute.domain.model.RouteInfo
import com.carnetroute.domain.port.RoutingPort
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.*
import org.slf4j.LoggerFactory
import kotlin.math.*

/**
 * Adaptateur de routage — appelle l'API OSRM publique avec fallback haversine.
 * Implémente [RoutingPort] pour que le domaine ne dépende d'aucun client HTTP.
 */
class OsrmRoutingAdapter : RoutingPort {

    private val logger = LoggerFactory.getLogger(OsrmRoutingAdapter::class.java)
    private val client = HttpClient(CIO)
    private val json   = Json { ignoreUnknownKeys = true }

    // Cache simple : évite un double appel OSRM pour simulate + heatmap consécutifs
    private var cacheKey:   String?    = null
    private var cacheValue: RouteInfo? = null

    companion object {
        private const val OSRM_URL      = "http://router.project-osrm.org/route/v1/driving"
        private const val EARTH_RADIUS  = 6371.0
        private const val ROAD_FACTOR   = 1.35
        private const val AVG_SPEED_KMH = 90.0
    }

    override suspend fun getRoute(from: Coordinates, to: Coordinates, avoidTolls: Boolean): RouteInfo {
        val key = "${from.lat},${from.lng}|${to.lat},${to.lng}|$avoidTolls"
        cacheValue?.let { if (cacheKey == key) return it }

        val result = try {
            callOsrm(from, to)
        } catch (e: Exception) {
            logger.warn("OSRM indisponible, fallback haversine : ${e.message}")
            haversineFallback(from, to)
        }

        cacheKey   = key
        cacheValue = result
        return result
    }

    private suspend fun callOsrm(from: Coordinates, to: Coordinates): RouteInfo {
        val url = "$OSRM_URL/${from.lng},${from.lat};${to.lng},${to.lat}?overview=full&geometries=geojson"

        val response: HttpResponse = client.get(url)
        if (!response.status.isSuccess()) {
            throw Exception("OSRM a renvoyé ${response.status}: ${response.bodyAsText()}")
        }

        val parsed = json.parseToJsonElement(response.bodyAsText()).jsonObject
        val route  = parsed["routes"]!!.jsonArray[0].jsonObject
        val coords = route["geometry"]!!.jsonObject["coordinates"]!!.jsonArray

        val geometry = coords.map { pt ->
            val pair = pt.jsonArray
            listOf(pair[0].jsonPrimitive.double, pair[1].jsonPrimitive.double)
        }

        return RouteInfo(
            distanceKm = route["distance"]!!.jsonPrimitive.double / 1000.0,
            durationMin= route["duration"]!!.jsonPrimitive.double / 60.0,
            source     = "osrm",
            geometry   = geometry
        )
    }

    private fun haversineFallback(from: Coordinates, to: Coordinates): RouteInfo {
        val dLat = Math.toRadians(to.lat - from.lat)
        val dLng = Math.toRadians(to.lng - from.lng)
        val a    = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(from.lat)) * cos(Math.toRadians(to.lat)) * sin(dLng / 2).pow(2)
        val dist = EARTH_RADIUS * 2 * atan2(sqrt(a), sqrt(1 - a)) * ROAD_FACTOR
        return RouteInfo(
            distanceKm = dist,
            durationMin= (dist / AVG_SPEED_KMH) * 60.0,
            source     = "haversine"
        )
    }
}
