package com.carnetroute.domain.history

import kotlinx.serialization.Serializable

@Serializable
data class MonthlyStats(
    val month: String,
    val journeys: Int,
    val distanceKm: Double,
    val costEur: Double
)

@Serializable
data class UserStatistics(
    val userId: String,
    val totalJourneys: Int,
    val totalDistanceKm: Double,
    val totalCostEur: Double,
    val totalDurationMinutes: Double,
    val carbonEmissionKg: Double,
    val mostUsedFuelType: String?,
    val monthlyStats: Map<String, MonthlyStats> = emptyMap()
)
