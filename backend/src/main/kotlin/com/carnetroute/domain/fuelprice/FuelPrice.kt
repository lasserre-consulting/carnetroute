package com.carnetroute.domain.fuelprice

import kotlinx.serialization.Serializable

@Serializable
data class FuelPrice(
    val fuelType: String,
    val pricePerUnit: Double,
    val currency: String = "EUR",
    val unit: String,
    val source: String,
    val updatedAt: String
)
