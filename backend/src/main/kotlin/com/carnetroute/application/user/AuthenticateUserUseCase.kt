package com.carnetroute.application.user

import com.carnetroute.domain.shared.DomainException
import com.carnetroute.domain.user.UserRepository
import com.carnetroute.infrastructure.security.JwtService
import com.carnetroute.infrastructure.security.PasswordEncoder

data class AuthenticateUserRequest(
    val email: String,
    val password: String
)

class AuthenticateUserUseCase(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtService: JwtService
) {
    suspend fun execute(request: AuthenticateUserRequest): AuthTokens {
        if (request.email.isBlank()) throw DomainException.ValidationError("email", "Email is required")
        if (request.password.isBlank()) throw DomainException.ValidationError("password", "Password is required")

        val user = userRepository.findByEmail(request.email.lowercase().trim())
            ?: throw DomainException.InvalidCredentials

        if (!passwordEncoder.matches(request.password, user.passwordHash)) {
            throw DomainException.InvalidCredentials
        }

        return AuthTokens(
            accessToken = jwtService.generateToken(user.id, user.email),
            refreshToken = jwtService.generateRefreshToken(user.id),
            userId = user.id
        )
    }
}
