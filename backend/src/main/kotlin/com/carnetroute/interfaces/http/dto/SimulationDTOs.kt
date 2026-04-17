package com.carnetroute.interfaces.http.dto

import kotlinx.serialization.Serializable

@Serializable
data class SimulateRequest(
    val fromLat: Double,
    val fromLng: Double,
    val fromLabel: String = "",
    val toLat: Double,
    val toLng: Double,
    val toLabel: String = "",
    val fuelType: String = "SP95",
    val trafficMode: String = "manual",
    val trafficFactor: Double = 1.0,
    val departureHour: Int? = null,
    val departureDay: Int? = null,
    val customPrices: Map<String, Double> = emptyMap(),
    val customConsumptions: Map<String, Double> = emptyMap(),
    val vehicleId: String? = null,
    val saveToHistory: Boolean = false
)

// ── Nested response matching the Angular frontend model ───────────────────────

@Serializable
data class CoordinatesResponse(val lat: Double, val lng: Double, val label: String)

@Serializable
data class RouteResponse(
    val from: CoordinatesResponse,
    val to: CoordinatesResponse,
    val distanceKm: Double,
    val durationMinutes: Double,
    val geometry: List<List<Double>> = emptyList()
)

@Serializable
data class TrafficResponse(val mode: String, val factor: Double)

@Serializable
data class ComparisonEntryResponse(
    val fuelType: String,
    val pricePerUnit: Double,
    val consumptionPer100km: Double,
    val fuelConsumed: Double,
    val totalCost: Double,
    val unit: String
)

@Serializable
data class CostBreakdownResponse(
    val fuelType: String,
    val pricePerUnit: Double,
    val consumptionPer100km: Double,
    val fuelConsumedTotal: Double,
    val costTotal: Double,
    val durationAdjustedMinutes: Double,
    val comparison: Map<String, ComparisonEntryResponse>
)

@Serializable
data class SimulationResponse(
    val id: String,
    val userId: String?,
    val vehicleId: String?,
    val route: RouteResponse,
    val traffic: TrafficResponse,
    val costs: CostBreakdownResponse,
    val createdAt: String
)

@Serializable
data class HeatmapRequest(
    val fromLat: Double? = null,
    val fromLng: Double? = null
)

@Serializable
data class HeatmapResponse(val matrix: List<List<Double>>)

@Serializable
data class AutocompleteRequest(val q: String, val limit: Int = 5)

@Serializable
data class PagedSimulationResponse(
    val content: List<SimulationResponse>,
    val totalElements: Long,
    val page: Int,
    val size: Int,
    val totalPages: Int
)

@Serializable
data class AddressSuggestionResponse(
    val label: String,
    val lat: Double,
    val lng: Double,
    val city: String = "",
    val postcode: String = ""
)
