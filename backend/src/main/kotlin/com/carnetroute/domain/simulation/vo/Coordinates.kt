package com.carnetroute.domain.simulation.vo

import kotlinx.serialization.Serializable

@Serializable
data class Coordinates(
    val lat: Double,
    val lng: Double,
    val label: String = ""
)
