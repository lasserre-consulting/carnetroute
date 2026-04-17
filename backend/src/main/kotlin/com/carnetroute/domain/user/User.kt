package com.carnetroute.domain.user

import com.carnetroute.domain.user.vo.Email
import com.carnetroute.domain.user.vo.PasswordHash
import com.carnetroute.domain.user.vo.UserPreferences
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class User(
    val id: String,
    val email: String,
    val passwordHash: String,
    val name: String,
    val preferences: UserPreferences = UserPreferences(),
    val createdAt: String,
    val updatedAt: String
) {
    companion object {
        fun create(email: Email, passwordHash: PasswordHash, name: String): User {
            val now = Clock.System.now().toString()
            return User(
                id = UUID.randomUUID().toString(),
                email = email.value,
                passwordHash = passwordHash.value,
                name = name,
                preferences = UserPreferences(),
                createdAt = now,
                updatedAt = now
            )
        }
    }
}
