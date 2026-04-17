package com.carnetroute.domain.simulation.vo

import kotlinx.serialization.Serializable

@Serializable
data class TrafficConditions(
    val mode: String = "manual",
    val factor: Double = 1.0,
    val departureTime: String? = null
)
