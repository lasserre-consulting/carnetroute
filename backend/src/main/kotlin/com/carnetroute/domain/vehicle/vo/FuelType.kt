package com.carnetroute.domain.vehicle.vo

import com.carnetroute.domain.shared.DomainException

enum class FuelType(
    val displayName: String,
    val defaultPricePerUnit: Double,
    val defaultConsumptionPer100km: Double,
    val unit: String
) {
    SP95("SP95 (E10)", 1.85, 7.0, "L"),
    SP98("SP98 (E5)", 1.96, 7.2, "L"),
    DIESEL("Diesel (B7)", 2.19, 5.8, "L"),
    E85("Éthanol E85", 0.73, 9.5, "L"),
    GPL("GPL", 1.05, 9.8, "L"),
    ELECTRIC("Électrique", 0.44, 17.0, "kWh");

    companion object {
        fun fromString(value: String): FuelType =
            values().find { it.name.equals(value, ignoreCase = true) }
                ?: throw DomainException.InvalidFuelType(value)
    }
}
