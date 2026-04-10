package com.carnetroute.routes

import com.carnetroute.models.*
import com.carnetroute.services.GeocodingService
import com.carnetroute.services.SimulationService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.simulationRoutes(
    simulationService: SimulationService,
    geocodingService: GeocodingService
) {
    route("/api") {

        /**
         * GET /api/fuels — List all fuel profiles
         */
        get("/fuels") {
            call.respond(SimulationService.FUEL_PROFILES.values.toList())
        }

        /**
         * POST /api/simulate — Run a route simulation
         */
        post("/simulate") {
            try {
                val request = call.receive<SimulationRequest>()
                val result = simulationService.simulate(request)
                call.respond(result)
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "Invalid request")))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Simulation failed: ${e.message}"))
            }
        }

        /**
         * POST /api/heatmap — Generate weekly traffic heatmap
         */
        post("/heatmap") {
            try {
                val request = call.receive<SimulationRequest>()
                val result = simulationService.generateHeatmap(
                    from = request.from,
                    to = request.to,
                    fuelType = request.fuelType,
                    avoidTolls = request.avoidTolls,
                    customPrice = request.customPrice,
                    customConsumption = request.customConsumption
                )
                call.respond(result)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Heatmap generation failed: ${e.message}"))
            }
        }

        /**
         * GET /api/geocode?q=... — Proxy to api-adresse.data.gouv.fr
         * Solves CORS issues for the Angular frontend
         */
        get("/geocode") {
            val query = call.request.queryParameters["q"]
            if (query.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Query parameter 'q' is required"))
                return@get
            }
            val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 7
            val result = geocodingService.autocomplete(query, limit)
            call.respondText(result, ContentType.Application.Json)
        }

        /**
         * GET /api/geocode/reverse?lat=...&lng=... — Reverse geocoding proxy
         */
        get("/geocode/reverse") {
            val lat = call.request.queryParameters["lat"]?.toDoubleOrNull()
            val lng = call.request.queryParameters["lng"]?.toDoubleOrNull()
            if (lat == null || lng == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "lat and lng parameters required"))
                return@get
            }
            val result = geocodingService.reverse(lat, lng)
            call.respondText(result, ContentType.Application.Json)
        }

        /**
         * GET /api/health — Health check for K8s probes
         */
        get("/health") {
            call.respond(mapOf("status" to "ok", "service" to "carnet-route", "version" to "1.0.0"))
        }
    }
}
