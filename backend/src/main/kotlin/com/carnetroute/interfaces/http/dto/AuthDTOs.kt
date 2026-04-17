package com.carnetroute.interfaces.http.dto

import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(val email: String, val password: String, val name: String)

@Serializable
data class LoginRequest(val email: String, val password: String)

@Serializable
data class RefreshRequest(val refreshToken: String)

@Serializable
data class AuthResponse(val accessToken: String, val refreshToken: String, val userId: String)

@Serializable
data class UserResponse(
    val id: String,
    val email: String,
    val name: String,
    val preferences: PreferencesResponse
)

@Serializable
data class PreferencesResponse(
    val defaultFuelType: String? = null,
    val alertsEnabled: Boolean = true,
    val theme: String = "light"
)

@Serializable
data class UpdateProfileRequest(
    val name: String? = null,
    val theme: String? = null,
    val defaultFuelType: String? = null,
    val alertsEnabled: Boolean? = null
)
