package com.carnetroute.infrastructure.security

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import java.util.Date

class JwtServiceImpl(
    private val secret: String,
    private val issuer: String,
    private val audience: String,
    private val expiresInSeconds: Long,
    private val refreshExpiresInSeconds: Long
) : JwtService {
    private val algorithm = Algorithm.HMAC256(secret)

    override fun generateToken(userId: String, email: String): String =
        JWT.create()
            .withIssuer(issuer)
            .withAudience(audience)
            .withClaim("userId", userId)
            .withClaim("email", email)
            .withExpiresAt(Date(System.currentTimeMillis() + expiresInSeconds * 1000))
            .sign(algorithm)

    override fun generateRefreshToken(userId: String): String =
        JWT.create()
            .withIssuer(issuer)
            .withClaim("userId", userId)
            .withClaim("type", "refresh")
            .withExpiresAt(Date(System.currentTimeMillis() + refreshExpiresInSeconds * 1000))
            .sign(algorithm)

    override fun verifyToken(token: String): JwtClaims? = try {
        val decoded = JWT.require(algorithm).withIssuer(issuer).build().verify(token)
        JwtClaims(
            userId = decoded.getClaim("userId").asString(),
            email = decoded.getClaim("email").asString()
        )
    } catch (e: Exception) { null }

    override fun verifyRefreshToken(token: String): String? = try {
        val decoded = JWT.require(algorithm).withIssuer(issuer).build().verify(token)
        if (decoded.getClaim("type").asString() == "refresh")
            decoded.getClaim("userId").asString()
        else null
    } catch (e: Exception) { null }
}
