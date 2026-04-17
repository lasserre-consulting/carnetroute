package com.carnetroute.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import kotlinx.serialization.Serializable

@Serializable
private data class AuthErrorResponse(val error: String)

fun Application.configureSecurity() {
    val config = environment.config

    val jwtSecret = config.property("app.jwt.secret").getString()
    val jwtIssuer = config.property("app.jwt.issuer").getString()
    val jwtAudience = config.property("app.jwt.audience").getString()

    val algorithm = Algorithm.HMAC256(jwtSecret)
    val verifier = JWT.require(algorithm)
        .withIssuer(jwtIssuer)
        .withAudience(jwtAudience)
        .build()

    install(Authentication) {
        jwt("auth-jwt") {
            realm = "carnetroute"
            this.verifier(verifier)
            validate { credential ->
                val userId = credential.payload.getClaim("userId").asString()
                if (!userId.isNullOrBlank()) {
                    JWTPrincipal(credential.payload)
                } else {
                    null
                }
            }
            challenge { _, _ ->
                call.respond(
                    HttpStatusCode.Unauthorized,
                    AuthErrorResponse("Token JWT manquant ou invalide")
                )
            }
        }
    }
}
