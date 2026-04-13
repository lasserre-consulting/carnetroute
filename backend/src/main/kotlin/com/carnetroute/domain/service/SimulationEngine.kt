package com.carnetroute.domain.service

import com.carnetroute.domain.model.*
import kotlin.math.*

/**
 * Moteur de simulation de trajet — logique purement fonctionnelle, sans dépendance externe.
 * Calculs de distance haversine, facteurs trafic, coût carburant, heatmap.
 */
class SimulationEngine {

    companion object {
        private const val EARTH_RADIUS_KM = 6371.0
        const val ROAD_FACTOR = 1.35
        private const val AVG_SPEED_KMH = 90.0

        val FUEL_PROFILES: Map<String, FuelProfile> = mapOf(
            "sp95"      to FuelProfile("sp95",      "SP95 (E10)",    "⛽",  7.0,  "L/100km", 1.85, "€/L",   "#F59E0B", "SP95"),
            "sp98"      to FuelProfile("sp98",      "SP98 (E5)",     "⛽",  7.2,  "L/100km", 1.96, "€/L",   "#FB923C", "SP98"),
            "diesel"    to FuelProfile("diesel",    "Diesel (B7)",   "🛢️", 5.8,  "L/100km", 2.19, "€/L",   "#3B82F6", "Diesel"),
            "e85"       to FuelProfile("e85",       "Éthanol E85",   "🌿", 9.5,  "L/100km", 0.73, "€/L",   "#84CC16", "E85"),
            "gpl"       to FuelProfile("gpl",       "GPL",           "🔵", 9.8,  "L/100km", 1.05, "€/L",   "#8B5CF6", "GPL"),
            "electrique" to FuelProfile("electrique", "Électrique", "⚡", 17.0, "kWh/100km", 0.44, "€/kWh", "#10B981", "Élec.")
        )

        private val WEEKDAY_PATTERN = doubleArrayOf(
            0.15, 0.10, 0.08, 0.08, 0.12, 0.30, 0.65, 0.90,
            1.00, 0.75, 0.55, 0.50, 0.55, 0.50, 0.52, 0.60,
            0.75, 0.95, 1.00, 0.80, 0.55, 0.40, 0.30, 0.20
        )
        private val WEEKEND_PATTERN = doubleArrayOf(
            0.08, 0.05, 0.05, 0.05, 0.05, 0.08, 0.12, 0.18,
            0.25, 0.35, 0.45, 0.52, 0.50, 0.45, 0.48, 0.50,
            0.52, 0.48, 0.40, 0.32, 0.25, 0.20, 0.15, 0.10
        )
        private val FRIDAY_BOOST = doubleArrayOf(
            0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
            0.0, 0.0, 0.0, 0.0, 0.0, 0.05, 0.1, 0.15,
            0.25, 0.2, 0.1, 0.0, 0.0, 0.0, 0.0, 0.0
        )
        private val SUNDAY_BOOST = doubleArrayOf(
            0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
            0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.05, 0.1,
            0.2, 0.25, 0.2, 0.1, 0.05, 0.0, 0.0, 0.0
        )
        private val MANUAL_TRAFFIC = listOf(
            Triple(1.0,  "Fluide",        "🟢"),
            Triple(1.25, "Modéré",        "🟡"),
            Triple(1.55, "Dense",         "🟠"),
            Triple(2.0,  "Embouteillage", "🔴")
        )
    }

    /** Haversine distance entre deux coordonnées GPS (en km). */
    fun haversineKm(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLng / 2).pow(2)
        return EARTH_RADIUS_KM * 2 * atan2(sqrt(a), sqrt(1 - a))
    }

    /** Distance routière estimée (ligne droite × facteur route). */
    fun roadDistanceKm(from: Coordinates, to: Coordinates): Double =
        haversineKm(from.lat, from.lng, to.lat, to.lng) * ROAD_FACTOR

    /**
     * Facteur de trafic pour un jour et une heure donnés.
     * Retourne entre 1.0 (fluide) et 2.0 (embouteillage).
     * @param day 0=Lundi … 6=Dimanche
     * @param hour 0..23
     */
    fun getTrafficFactor(day: Int, hour: Int): Double {
        val isWeekend = day >= 5
        val base = if (isWeekend) WEEKEND_PATTERN[hour] else WEEKDAY_PATTERN[hour]
        val friday = if (day == 4) FRIDAY_BOOST[hour] else 0.0
        val sunday = if (day == 6) SUNDAY_BOOST[hour] else 0.0
        return 1.0 + minOf(base + friday + sunday, 1.0)
    }

    /** Label et icône correspondant au facteur de trafic. */
    fun getTrafficInfo(factor: Double): Pair<String, String> = when {
        factor < 1.15 -> "Fluide"        to "🟢"
        factor < 1.40 -> "Modéré"        to "🟡"
        factor < 1.70 -> "Dense"         to "🟠"
        else          -> "Embouteillage" to "🔴"
    }

    /**
     * Calcule la simulation complète à partir d'une route déjà résolue.
     * @throws IllegalArgumentException si le type de carburant est inconnu
     */
    fun simulate(request: SimulationRequest, route: RouteInfo): SimulationResult {
        val profile = FUEL_PROFILES[request.fuelType]
            ?: throw IllegalArgumentException("Type de carburant inconnu : ${request.fuelType}")

        val distance   = route.distanceKm
        val baseTime   = route.durationMin

        val (trafficFactor, trafficLabel, trafficIcon) = resolveTraffic(request)

        val adjustedTime = baseTime * trafficFactor
        val price        = request.customPrice ?: profile.defaultPrice
        val consumption  = request.customConsumption ?: profile.consumption
        val extraConsRate = if (request.fuelType == "electrique") 0.08 else 0.12
        val adjCons      = consumption * (1 + (trafficFactor - 1) * extraConsRate)
        val fuelConsumed = (adjCons * distance) / 100.0
        val totalCost    = fuelConsumed * price
        val costPer100km = if (distance > 0) (totalCost / distance) * 100 else 0.0
        val avgSpeed     = if (adjustedTime > 0) distance / (adjustedTime / 60.0) else 0.0
        val costPerHour  = if (adjustedTime > 0) totalCost / (adjustedTime / 60.0) else 0.0

        val comparison = FUEL_PROFILES.map { (key, p) ->
            val rate = if (key == "electrique") 0.08 else 0.12
            val ac   = p.consumption * (1 + (trafficFactor - 1) * rate)
            val cost = (ac * distance / 100.0) * p.defaultPrice
            FuelComparison(key, p.label, p.icon, p.color, round2(cost), ac, p.unit)
        }

        return SimulationResult(
            distanceKm     = round2(distance),
            baseTimeMin    = round2(baseTime),
            adjustedTimeMin= round2(adjustedTime),
            trafficFactor  = round2(trafficFactor),
            trafficLabel   = trafficLabel,
            trafficIcon    = trafficIcon,
            fuelConsumed   = round2(fuelConsumed),
            fuelUnit       = if (request.fuelType == "electrique") "kWh" else "L",
            totalCost      = round2(totalCost),
            costPer100km   = round2(costPer100km),
            avgSpeedKmh    = round2(avgSpeed),
            costPerHour    = round2(costPerHour),
            comparison     = comparison,
            routingSource  = route.source,
            routeGeometry  = route.geometry
        )
    }

    /**
     * Génère la grille heatmap 7 jours × 24 heures pour une route donnée.
     * @throws IllegalArgumentException si le type de carburant est inconnu
     */
    fun generateHeatmapGrid(
        route: RouteInfo,
        fuelType: String,
        customPrice: Double? = null,
        customConsumption: Double? = null
    ): WeeklyHeatmapResult {
        val profile = FUEL_PROFILES[fuelType]
            ?: throw IllegalArgumentException("Type de carburant inconnu : $fuelType")

        val distance    = route.distanceKm
        val baseTime    = route.durationMin
        val price       = customPrice ?: profile.defaultPrice
        val consumption = customConsumption ?: profile.consumption
        val extraConsRate = if (fuelType == "electrique") 0.08 else 0.12

        val grid = (0 until 7).map { day ->
            (0 until 24).map { hour ->
                val factor   = getTrafficFactor(day, hour)
                val duration = baseTime * factor
                val adjCons  = consumption * (1 + (factor - 1) * extraConsRate)
                val cost     = (adjCons * distance / 100.0) * price
                HeatmapCell(day, hour, round2(duration), round2(factor), round2(cost))
            }
        }

        return WeeklyHeatmapResult(
            baseTimeMin = round2(baseTime),
            distanceKm  = round2(distance),
            grid        = grid
        )
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private data class TrafficResult(val factor: Double, val label: String, val icon: String)

    private fun resolveTraffic(request: SimulationRequest): TrafficResult {
        return if (request.trafficMode == "manual") {
            val t = MANUAL_TRAFFIC.getOrElse(request.manualTrafficLevel) { MANUAL_TRAFFIC[0] }
            TrafficResult(t.first, t.second, t.third)
        } else {
            val factor = getTrafficFactor(request.departureDay, request.departureHour)
            val (label, icon) = getTrafficInfo(factor)
            TrafficResult(factor, label, icon)
        }
    }

    private fun round2(value: Double): Double = Math.round(value * 100) / 100.0
}
