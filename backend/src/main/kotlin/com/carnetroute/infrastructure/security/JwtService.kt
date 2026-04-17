package com.carnetroute.infrastructure.security

interface JwtService {
    fun generateToken(userId: String, email: String): String
    fun generateRefreshToken(userId: String): String
    fun verifyToken(token: String): JwtClaims?
    fun verifyRefreshToken(token: String): String? // returns userId
}

data class JwtClaims(val userId: String, val email: String)
