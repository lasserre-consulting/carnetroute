package com.carnetroute.infrastructure.routing

import com.carnetroute.domain.simulation.vo.Coordinates
import com.carnetroute.domain.simulation.vo.Route
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

object HaversineCalculator : RoutingPort {
    private const val EARTH_RADIUS_KM = 6371.0
    private const val ROAD_FACTOR = 1.3  // facteur route vs vol d'oiseau
    private const val AVG_SPEED_KMH = 80.0

    override suspend fun getRoute(from: Coordinates, to: Coordinates): Route {
        val distanceKm = calculateDistance(from, to) * ROAD_FACTOR
        val durationMinutes = (distanceKm / AVG_SPEED_KMH) * 60.0
        return Route(
            from = from,
            to = to,
            distanceKm = distanceKm,
            durationMinutes = durationMinutes,
            geometry = emptyList()
        )
    }

    fun calculateDistance(from: Coordinates, to: Coordinates): Double {
        val dLat = Math.toRadians(to.lat - from.lat)
        val dLng = Math.toRadians(to.lng - from.lng)
        val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(from.lat)) * cos(Math.toRadians(to.lat)) * sin(dLng / 2).pow(2)
        return 2 * EARTH_RADIUS_KM * atan2(sqrt(a), sqrt(1 - a))
    }
}
