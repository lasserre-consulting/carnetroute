package com.carnetroute.domain.vehicle

import com.carnetroute.domain.vehicle.vo.FuelProfile
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class Vehicle(
    val id: String,
    val userId: String,
    val name: String,
    val fuelProfile: FuelProfile,
    val tankCapacity: Double = 50.0,
    val emissionsGCO2PerKm: Double = 120.0,
    val yearMake: Int = 2020,
    val isDefault: Boolean = false,
    val createdAt: String,
    val updatedAt: String
) {
    companion object {
        fun create(
            userId: String,
            name: String,
            fuelProfile: FuelProfile,
            tankCapacity: Double = 50.0,
            emissionsGCO2PerKm: Double = 120.0,
            yearMake: Int = 2020,
            isDefault: Boolean = false
        ): Vehicle {
            val now = Clock.System.now().toString()
            return Vehicle(
                id = UUID.randomUUID().toString(),
                userId = userId,
                name = name,
                fuelProfile = fuelProfile,
                tankCapacity = tankCapacity,
                emissionsGCO2PerKm = emissionsGCO2PerKm,
                yearMake = yearMake,
                isDefault = isDefault,
                createdAt = now,
                updatedAt = now
            )
        }
    }
}
