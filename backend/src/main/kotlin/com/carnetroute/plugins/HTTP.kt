package com.carnetroute.plugins

import com.carnetroute.domain.shared.DomainException
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.defaultheaders.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import kotlinx.serialization.Serializable
import org.slf4j.event.Level
import java.util.UUID

@Serializable
private data class ErrorResponse(val error: String)

fun Application.configureHTTP() {
    val config = environment.config

    val allowedOrigins = config.propertyOrNull("app.cors.allowedOrigins")
        ?.getList()
        ?: listOf("http://localhost:4200")

    // CORS
    install(CORS) {
        allowedOrigins.forEach { origin ->
            allowHost(
                host = origin
                    .removePrefix("https://")
                    .removePrefix("http://"),
                schemes = listOf(if (origin.startsWith("https")) "https" else "http"),
            )
        }
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Patch)
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        allowCredentials = true
    }

    // Default headers
    install(DefaultHeaders) {
        header("X-Frame-Options", "DENY")
        header("X-Content-Type-Options", "nosniff")
        header("Strict-Transport-Security", "max-age=31536000; includeSubDomains")
        header("X-Request-Id", UUID.randomUUID().toString())
    }

    // Status pages : mapping DomainException → codes HTTP appropriés
    install(StatusPages) {
        // Erreurs 404 Not Found
        exception<DomainException.UserNotFound> { call, cause ->
            call.respond(HttpStatusCode.NotFound, ErrorResponse(cause.message ?: "Utilisateur introuvable"))
        }
        exception<DomainException.VehicleNotFound> { call, cause ->
            call.respond(HttpStatusCode.NotFound, ErrorResponse(cause.message ?: "Véhicule introuvable"))
        }
        exception<DomainException.SimulationNotFound> { call, cause ->
            call.respond(HttpStatusCode.NotFound, ErrorResponse(cause.message ?: "Simulation introuvable"))
        }

        // Erreurs 401 Unauthorized
        exception<DomainException.InvalidCredentials> { call, _ ->
            call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Identifiants invalides"))
        }
        exception<DomainException.Unauthorized> { call, cause ->
            call.respond(HttpStatusCode.Unauthorized, ErrorResponse(cause.message ?: "Accès non autorisé"))
        }

        // Erreurs 409 Conflict
        exception<DomainException.UserAlreadyExists> { call, cause ->
            call.respond(HttpStatusCode.Conflict, ErrorResponse(cause.message ?: "Utilisateur déjà existant"))
        }

        // Erreurs 400 Bad Request
        exception<DomainException.ValidationError> { call, cause ->
            call.respond(HttpStatusCode.BadRequest, ErrorResponse(cause.message ?: "Erreur de validation"))
        }
        exception<DomainException.InvalidFuelType> { call, cause ->
            call.respond(HttpStatusCode.BadRequest, ErrorResponse(cause.message ?: "Type de carburant invalide"))
        }

        // Erreurs 502 Bad Gateway (service externe)
        exception<DomainException.RoutingError> { call, cause ->
            call.respond(HttpStatusCode.BadGateway, ErrorResponse(cause.message ?: "Erreur de calcul d'itinéraire"))
        }

        // Toute autre DomainException → 500
        exception<DomainException> { call, cause ->
            call.application.environment.log.error("DomainException non gérée", cause)
            call.respond(HttpStatusCode.InternalServerError, ErrorResponse(cause.message ?: "Erreur interne"))
        }

        // Exceptions non prévues → 500
        exception<Throwable> { call, cause ->
            call.application.environment.log.error("Erreur inattendue", cause)
            call.respond(
                HttpStatusCode.InternalServerError,
                ErrorResponse("Une erreur interne est survenue"),
            )
        }
    }

    // Call logging
    install(CallLogging) {
        level = Level.INFO
        format { call ->
            val status = call.response.status()
            val method = call.request.httpMethod.value
            val path = call.request.uri
            val duration = call.processingTimeMillis()
            "$method $path → ${status?.value} (${duration}ms)"
        }
    }
}
