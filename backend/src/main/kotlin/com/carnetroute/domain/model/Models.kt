package com.carnetroute.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Coordinates(
    val lat: Double,
    val lng: Double,
    val label: String = ""
)

@Serializable
data class SimulationRequest(
    val from: Coordinates,
    val to: Coordinates,
    val fuelType: String,
    val customPrice: Double? = null,
    val customConsumption: Double? = null,
    val trafficMode: String = "auto", // "manual" | "auto"
    val manualTrafficLevel: Int = 0,  // 0-3 for manual
    val departureDay: Int = 0,        // 0=Mon..6=Sun for auto
    val departureHour: Int = 8,       // 0-23 for auto
    val avoidTolls: Boolean = false
)

@Serializable
data class SimulationResult(
    val distanceKm: Double,
    val baseTimeMin: Double,
    val adjustedTimeMin: Double,
    val trafficFactor: Double,
    val trafficLabel: String,
    val trafficIcon: String,
    val fuelConsumed: Double,
    val fuelUnit: String,
    val totalCost: Double,
    val costPer100km: Double,
    val avgSpeedKmh: Double,
    val costPerHour: Double,
    val comparison: List<FuelComparison>,
    val routingSource: String = "haversine",
    val routeGeometry: List<List<Double>> = emptyList()
)

@Serializable
data class FuelComparison(
    val key: String,
    val label: String,
    val icon: String,
    val color: String,
    val cost: Double,
    val consumption: Double,
    val unit: String
)

@Serializable
data class WeeklyHeatmapResult(
    val baseTimeMin: Double,
    val distanceKm: Double,
    val grid: List<List<HeatmapCell>>
)

@Serializable
data class HeatmapCell(
    val day: Int,
    val hour: Int,
    val durationMin: Double,
    val trafficFactor: Double,
    val cost: Double
)

@Serializable
data class FuelProfile(
    val key: String,
    val label: String,
    val icon: String,
    val consumption: Double,
    val unit: String,
    val defaultPrice: Double,
    val priceUnit: String,
    val color: String,
    val shortName: String
)

@Serializable
data class RouteInfo(
    val distanceKm: Double,
    val durationMin: Double,
    val source: String,
    val geometry: List<List<Double>> = emptyList()
)

@Serializable
data class FuelPriceAlert(
    val id: String,
    val fuelType: String,
    val fuelLabel: String,
    val fuelIcon: String,
    val currentPrice: Double,
    val previousPrice: Double,
    val changePercent: Double,
    val direction: String,
    val severity: String,
    val message: String,
    val timestamp: Long
)

@Serializable
data class LiveFuelPrices(
    val prices: Map<String, Double>,
    val lastUpdate: Long,
    val alerts: List<FuelPriceAlert> = emptyList()
)
