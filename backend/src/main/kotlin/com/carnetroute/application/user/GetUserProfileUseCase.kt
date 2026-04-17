package com.carnetroute.application.user

import com.carnetroute.domain.shared.DomainException
import com.carnetroute.domain.user.User
import com.carnetroute.domain.user.UserRepository

class GetUserProfileUseCase(
    private val userRepository: UserRepository
) {
    suspend fun execute(userId: String): User {
        return userRepository.findById(userId)
            ?: throw DomainException.UserNotFound(
                runCatching { java.util.UUID.fromString(userId) }.getOrElse { java.util.UUID.nameUUIDFromBytes(userId.toByteArray()) }
            )
    }
}
