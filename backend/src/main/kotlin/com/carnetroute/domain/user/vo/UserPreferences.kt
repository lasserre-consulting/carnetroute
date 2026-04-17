package com.carnetroute.domain.user.vo

import kotlinx.serialization.Serializable

@Serializable
data class UserPreferences(
    val defaultFuelType: String? = null,
    val defaultVehicleId: String? = null,
    val alertsEnabled: Boolean = true,
    val theme: String = "light"
)
