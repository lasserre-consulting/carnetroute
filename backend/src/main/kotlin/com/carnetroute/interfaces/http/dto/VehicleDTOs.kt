package com.carnetroute.interfaces.http.dto

import kotlinx.serialization.Serializable

@Serializable
data class CreateVehicleRequest(
    val name: String,
    val fuelType: String,
    val consumptionPer100km: Double? = null,
    val costPerUnit: Double? = null,
    val tankCapacity: Double? = null,
    val yearMake: Int? = null,
    val isDefault: Boolean = false
)

@Serializable
data class UpdateVehicleRequest(
    val name: String? = null,
    val consumptionPer100km: Double? = null,
    val costPerUnit: Double? = null,
    val isDefault: Boolean? = null
)

@Serializable
data class VehicleResponse(
    val id: String,
    val userId: String,
    val name: String,
    val fuelType: String,
    val consumptionPer100km: Double,
    val costPerUnit: Double,
    val tankCapacity: Double,
    val yearMake: Int,
    val isDefault: Boolean,
    val createdAt: String
)
