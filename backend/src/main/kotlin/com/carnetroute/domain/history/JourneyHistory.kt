package com.carnetroute.domain.history

import kotlinx.serialization.Serializable

@Serializable
data class JourneyHistory(
    val id: String,
    val userId: String,
    val simulationId: String,
    val fuelType: String,
    val distanceKm: Double,
    val costTotal: Double,
    val durationMinutes: Double,
    val carbonEmissionKg: Double,
    val createdAt: String,
    val tags: List<String> = emptyList()
)
