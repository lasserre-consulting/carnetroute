package com.carnetroute.interfaces.http

import com.carnetroute.application.history.GetJourneyHistoryUseCase
import com.carnetroute.application.history.GetUserStatisticsUseCase
import com.carnetroute.domain.history.JourneyHistory
import com.carnetroute.domain.history.UserStatistics
import com.carnetroute.interfaces.http.dto.JourneyHistoryResponse
import com.carnetroute.interfaces.http.dto.MonthlyStatsResponse
import com.carnetroute.interfaces.http.dto.PagedHistoryResponse
import com.carnetroute.interfaces.http.dto.StatsResponse
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
fun Route.configureHistoryRoutes(
    getJourneyHistoryUseCase: GetJourneyHistoryUseCase,
    getUserStatisticsUseCase: GetUserStatisticsUseCase,
) {

    authenticate("auth-jwt") {
        route("/history") {
            // GET /api/history — historique des trajets
            get {
                val userId = call.principal<JWTPrincipal>()!!.payload.getClaim("userId").asString()
                val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 0
                val size = call.request.queryParameters["size"]?.toIntOrNull() ?: 20
                val pageResult = getJourneyHistoryUseCase.execute(userId, page, size)
                call.respond(PagedHistoryResponse(
                    content = pageResult.content.map { it.toResponse() },
                    totalElements = pageResult.totalElements,
                    page = pageResult.page,
                    size = pageResult.size,
                    totalPages = pageResult.totalPages
                ))
            }
        }

        // GET /api/stats — statistiques utilisateur
        get("/stats") {
            val userId = call.principal<JWTPrincipal>()!!.payload.getClaim("userId").asString()
            val stats = getUserStatisticsUseCase.execute(userId)
            call.respond(stats.toResponse())
        }
    }
}

private fun JourneyHistory.toResponse(): JourneyHistoryResponse = JourneyHistoryResponse(
    id = id,
    simulationId = simulationId,
    fuelType = fuelType,
    distanceKm = distanceKm,
    costTotal = costTotal,
    durationMinutes = durationMinutes,
    carbonEmissionKg = carbonEmissionKg,
    createdAt = createdAt,
    tags = tags
)

private fun UserStatistics.toResponse(): StatsResponse = StatsResponse(
    totalJourneys = totalJourneys,
    totalDistanceKm = totalDistanceKm,
    totalCostEur = totalCostEur,
    totalDurationMinutes = totalDurationMinutes,
    carbonEmissionKg = carbonEmissionKg,
    mostUsedFuelType = mostUsedFuelType,
    monthlyStats = monthlyStats.mapValues { (month, stat) ->
        MonthlyStatsResponse(
            month = month,
            journeys = stat.journeys,
            distanceKm = stat.distanceKm,
            costEur = stat.costEur
        )
    }
)
