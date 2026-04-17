package com.carnetroute.interfaces.http.dto

import kotlinx.serialization.Serializable

@Serializable
data class FuelPriceResponse(
    val fuelType: String,
    val displayName: String,
    val pricePerUnit: Double,
    val unit: String,
    val defaultConsumption: Double,
    val source: String,
    val updatedAt: String
)

@Serializable
data class FuelPricesResponse(
    val prices: List<FuelPriceResponse>,
    val updatedAt: String
)
