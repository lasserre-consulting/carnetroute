package com.carnetroute.interfaces.http

import com.carnetroute.application.geocoding.AutocompleteUseCase
import com.carnetroute.application.simulation.GenerateHeatmapUseCase
import com.carnetroute.application.simulation.GetSimulationHistoryUseCase
import com.carnetroute.application.simulation.SimulateRouteUseCase
import com.carnetroute.application.simulation.SimulateRouteRequest as DomainSimulateRequest
import com.carnetroute.domain.simulation.Simulation
import com.carnetroute.interfaces.http.dto.AddressSuggestionResponse
import com.carnetroute.interfaces.http.dto.ComparisonEntryResponse
import com.carnetroute.interfaces.http.dto.CoordinatesResponse
import com.carnetroute.interfaces.http.dto.CostBreakdownResponse
import com.carnetroute.interfaces.http.dto.HeatmapResponse
import com.carnetroute.interfaces.http.dto.PagedSimulationResponse
import com.carnetroute.interfaces.http.dto.RouteResponse
import com.carnetroute.interfaces.http.dto.SimulateRequest
import com.carnetroute.interfaces.http.dto.SimulationResponse
import com.carnetroute.interfaces.http.dto.TrafficResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
fun Route.configureSimulationRoutes(
    simulateRouteUseCase: SimulateRouteUseCase,
    generateHeatmapUseCase: GenerateHeatmapUseCase,
    getSimulationHistoryUseCase: GetSimulationHistoryUseCase,
    autocompleteUseCase: AutocompleteUseCase,
) {

    // POST /api/simulate — userId optionnel (utilisateur connecté ou anonyme)
    authenticate("auth-jwt", optional = true) {
    post("/simulate") {
        val request = call.receive<SimulateRequest>()
        val userId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asString()

        val simulation = simulateRouteUseCase.execute(
            DomainSimulateRequest(
                fromLat = request.fromLat,
                fromLng = request.fromLng,
                fromLabel = request.fromLabel,
                toLat = request.toLat,
                toLng = request.toLng,
                toLabel = request.toLabel,
                fuelType = request.fuelType,
                trafficMode = request.trafficMode,
                trafficFactor = request.trafficFactor,
                departureHour = request.departureHour,
                departureDay = request.departureDay,
                customPrices = request.customPrices,
                customConsumptions = request.customConsumptions,
                vehicleId = request.vehicleId,
                userId = userId,
                saveToHistory = request.saveToHistory
            )
        )
        call.respond(HttpStatusCode.OK, simulation.toResponse())
    }
    } // end authenticate optional

    // POST /api/heatmap
    post("/heatmap") {
        val result = generateHeatmapUseCase.execute()
        val matrix: List<List<Double>> = result.matrix.map { row -> row.toList() }
        call.respond(HeatmapResponse(matrix = matrix))
    }

    // GET /api/simulations — historique des simulations (authentifié)
    authenticate("auth-jwt") {
        get("/simulations") {
            val userId = call.principal<JWTPrincipal>()!!.payload.getClaim("userId").asString()
            val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 0
            val size = call.request.queryParameters["size"]?.toIntOrNull() ?: 20
            val pageResult = getSimulationHistoryUseCase.execute(userId, page, size)
            call.respond(PagedSimulationResponse(
                content = pageResult.content.map { it.toResponse() },
                totalElements = pageResult.totalElements,
                page = pageResult.page,
                size = pageResult.size,
                totalPages = pageResult.totalPages
            ))
        }
    }

    // GET /api/geocode — autocomplétion adresses
    get("/geocode") {
        val q = call.request.queryParameters["q"] ?: run {
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing query parameter 'q'"))
            return@get
        }
        val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 5
        val suggestions = autocompleteUseCase.execute(q, limit)
        call.respond(
            suggestions.map { s ->
                AddressSuggestionResponse(
                    label = s.label,
                    lat = s.lat,
                    lng = s.lng,
                    city = s.city,
                    postcode = s.postcode
                )
            }
        )
    }
}

private fun Simulation.toResponse(): SimulationResponse = SimulationResponse(
    id = id,
    userId = userId,
    vehicleId = vehicleId,
    route = RouteResponse(
        from = CoordinatesResponse(route.from.lat, route.from.lng, route.from.label),
        to = CoordinatesResponse(route.to.lat, route.to.lng, route.to.label),
        distanceKm = route.distanceKm,
        durationMinutes = route.durationMinutes,
        geometry = route.geometry
    ),
    traffic = TrafficResponse(mode = traffic.mode, factor = traffic.factor),
    costs = CostBreakdownResponse(
        fuelType = costs.fuelType,
        pricePerUnit = costs.pricePerUnit,
        consumptionPer100km = costs.consumptionPer100km,
        fuelConsumedTotal = costs.fuelConsumedTotal,
        costTotal = costs.costTotal,
        durationAdjustedMinutes = costs.durationAdjustedMinutes,
        comparison = costs.comparison.mapValues { (_, e) ->
            ComparisonEntryResponse(e.fuelType, e.pricePerUnit, e.consumptionPer100km, e.fuelConsumed, e.totalCost, e.unit)
        }
    ),
    createdAt = createdAt
)
