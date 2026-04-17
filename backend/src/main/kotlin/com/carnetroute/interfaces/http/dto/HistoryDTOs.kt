package com.carnetroute.interfaces.http.dto

import kotlinx.serialization.Serializable

@Serializable
data class JourneyHistoryResponse(
    val id: String,
    val simulationId: String,
    val fuelType: String,
    val distanceKm: Double,
    val costTotal: Double,
    val durationMinutes: Double,
    val carbonEmissionKg: Double,
    val createdAt: String,
    val tags: List<String>
)

@Serializable
data class StatsResponse(
    val totalJourneys: Int,
    val totalDistanceKm: Double,
    val totalCostEur: Double,
    val totalDurationMinutes: Double,
    val carbonEmissionKg: Double,
    val mostUsedFuelType: String?,
    val monthlyStats: Map<String, MonthlyStatsResponse>
)

@Serializable
data class MonthlyStatsResponse(
    val month: String,
    val journeys: Int,
    val distanceKm: Double,
    val costEur: Double
)

@Serializable
data class PagedHistoryResponse(
    val content: List<JourneyHistoryResponse>,
    val totalElements: Long,
    val page: Int,
    val size: Int,
    val totalPages: Int
)
