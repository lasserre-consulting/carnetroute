package com.carnetroute.application.user

import com.carnetroute.domain.shared.DomainException
import com.carnetroute.domain.user.User
import com.carnetroute.domain.user.UserRepository
import com.carnetroute.domain.user.vo.Email
import com.carnetroute.domain.user.vo.PasswordHash
import com.carnetroute.infrastructure.security.JwtService
import com.carnetroute.infrastructure.security.PasswordEncoder

data class CreateUserRequest(
    val email: String,
    val password: String,
    val name: String
)

data class AuthTokens(
    val accessToken: String,
    val refreshToken: String,
    val userId: String
)

class CreateUserUseCase(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtService: JwtService
) {
    suspend fun execute(request: CreateUserRequest): AuthTokens {
        if (request.email.isBlank()) throw DomainException.ValidationError("email", "Email is required")
        if (request.name.isBlank()) throw DomainException.ValidationError("name", "Name is required")
        if (request.password.length < 8) throw DomainException.ValidationError("password", "Password must be at least 8 characters")

        val normalizedEmail = request.email.lowercase().trim()

        // Email.of() validates format; wrap any ValidationError it throws
        val emailVo = Email.of(normalizedEmail)

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw DomainException.UserAlreadyExists(normalizedEmail)
        }

        val hash = passwordEncoder.encode(request.password)
        val user = User.create(
            email = emailVo,
            passwordHash = PasswordHash(hash),
            name = request.name
        )
        val saved = userRepository.save(user)

        return AuthTokens(
            accessToken = jwtService.generateToken(saved.id, saved.email),
            refreshToken = jwtService.generateRefreshToken(saved.id),
            userId = saved.id
        )
    }
}
