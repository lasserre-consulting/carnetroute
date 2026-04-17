package com.carnetroute.domain.simulation.vo

import kotlinx.serialization.Serializable

@Serializable
data class Route(
    val from: Coordinates,
    val to: Coordinates,
    val distanceKm: Double,
    val durationMinutes: Double,
    val geometry: List<List<Double>> = emptyList()
)
