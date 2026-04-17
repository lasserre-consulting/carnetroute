package com.carnetroute.application.user

import com.carnetroute.domain.shared.DomainException
import com.carnetroute.domain.user.User
import com.carnetroute.domain.user.UserRepository
import com.carnetroute.domain.user.vo.UserPreferences
import kotlinx.datetime.Clock

data class UserPreferencesRequest(
    val defaultFuelType: String? = null,
    val defaultVehicleId: String? = null,
    val alertsEnabled: Boolean? = null,
    val theme: String? = null
)

data class UpdateProfileRequest(
    val name: String? = null,
    val preferences: UserPreferencesRequest? = null
)

class UpdateUserProfileUseCase(
    private val userRepository: UserRepository
) {
    suspend fun execute(userId: String, request: UpdateProfileRequest): User {
        val existing = userRepository.findById(userId)
            ?: throw DomainException.UserNotFound(
                runCatching { java.util.UUID.fromString(userId) }.getOrElse { java.util.UUID.nameUUIDFromBytes(userId.toByteArray()) }
            )

        val updatedPreferences = request.preferences?.let { req ->
            existing.preferences.copy(
                defaultFuelType = req.defaultFuelType ?: existing.preferences.defaultFuelType,
                defaultVehicleId = req.defaultVehicleId ?: existing.preferences.defaultVehicleId,
                alertsEnabled = req.alertsEnabled ?: existing.preferences.alertsEnabled,
                theme = req.theme ?: existing.preferences.theme
            )
        } ?: existing.preferences

        val updated = existing.copy(
            name = request.name ?: existing.name,
            preferences = updatedPreferences,
            updatedAt = Clock.System.now().toString()
        )

        return userRepository.update(updated)
    }
}
