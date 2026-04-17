package com.carnetroute.domain.vehicle.vo

import kotlinx.serialization.Serializable

@Serializable
data class FuelProfile(
    val fuelType: String,
    val consumptionPer100km: Double,
    val costPerUnit: Double
)
