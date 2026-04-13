package com.carnetroute.infrastructure.routes

import com.carnetroute.application.usecase.SimulationUseCases
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.simulationRoutes(useCases: SimulationUseCases) {
    route("/api") {

        /** GET /api/fuels — Liste des profils carburant */
        get("/fuels") {
            call.respond(useCases.fuels.execute())
        }

        /** POST /api/simulate — Simulation de trajet */
        post("/simulate") {
            try {
                val request = call.receive<com.carnetroute.domain.model.SimulationRequest>()
                call.respond(useCases.simulate.execute(request))
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "Requête invalide")))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Erreur simulation : ${e.message}"))
            }
        }

        /** POST /api/heatmap — Heatmap hebdomadaire */
        post("/heatmap") {
            try {
                val request = call.receive<com.carnetroute.domain.model.SimulationRequest>()
                call.respond(useCases.heatmap.execute(request))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Erreur heatmap : ${e.message}"))
            }
        }

        /** GET /api/geocode?q=... — Proxy autocomplétion */
        get("/geocode") {
            val query = call.request.queryParameters["q"]
            if (query.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Paramètre 'q' requis"))
                return@get
            }
            val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 7
            val result = useCases.geocode.execute(query, limit)
            call.respondText(result, ContentType.Application.Json)
        }

        /** GET /api/geocode/reverse?lat=...&lng=... — Géocodage inverse */
        get("/geocode/reverse") {
            val lat = call.request.queryParameters["lat"]?.toDoubleOrNull()
            val lng = call.request.queryParameters["lng"]?.toDoubleOrNull()
            if (lat == null || lng == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Paramètres lat et lng requis"))
                return@get
            }
            val result = useCases.reverseGeocode.execute(lat, lng)
            call.respondText(result, ContentType.Application.Json)
        }

        /** GET /api/health — Health check pour les probes K8s */
        get("/health") {
            call.respond(mapOf("status" to "ok", "service" to "carnet-route", "version" to "1.0.0"))
        }
    }
}
