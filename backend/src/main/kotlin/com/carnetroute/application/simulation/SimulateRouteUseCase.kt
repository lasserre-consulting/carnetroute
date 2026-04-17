package com.carnetroute.application.simulation

import com.carnetroute.domain.history.JourneyHistory
import com.carnetroute.domain.history.HistoryRepository
import com.carnetroute.domain.shared.DomainException
import com.carnetroute.domain.simulation.Simulation
import com.carnetroute.domain.simulation.SimulationEngine
import com.carnetroute.domain.simulation.SimulationRepository
import com.carnetroute.domain.simulation.vo.Coordinates
import com.carnetroute.domain.vehicle.vo.FuelType
import com.carnetroute.infrastructure.routing.RoutingPort
import kotlinx.datetime.Clock
import java.util.UUID

data class SimulateRouteRequest(
    val fromLat: Double,
    val fromLng: Double,
    val fromLabel: String = "",
    val toLat: Double,
    val toLng: Double,
    val toLabel: String = "",
    val fuelType: String,
    val trafficMode: String = "manual",
    val trafficFactor: Double = 1.0,
    val departureHour: Int? = null,
    val departureDay: Int? = null,
    val customPrices: Map<String, Double> = emptyMap(),
    val customConsumptions: Map<String, Double> = emptyMap(),
    val vehicleId: String? = null,
    val userId: String? = null,
    val saveToHistory: Boolean = false,
)

class SimulateRouteUseCase(
    private val routingPort: RoutingPort,
    private val simulationEngine: SimulationEngine,
    private val simulationRepository: SimulationRepository,
    private val historyRepository: HistoryRepository,
) {
    suspend fun execute(request: SimulateRouteRequest): Simulation {
        val fuelType = FuelType.fromString(request.fuelType)

        val from = Coordinates(lat = request.fromLat, lng = request.fromLng, label = request.fromLabel)
        val to = Coordinates(lat = request.toLat, lng = request.toLng, label = request.toLabel)

        val route = routingPort.getRoute(from, to)

        val resolvedTrafficFactor = when (request.trafficMode) {
            "auto" -> {
                val day = request.departureDay
                val hour = request.departureHour
                if (day != null && hour != null) {
                    simulationEngine.calculateTrafficFactor(dayOfWeek = day, hour = hour)
                } else {
                    request.trafficFactor
                }
            }
            else -> request.trafficFactor
        }

        // Convertir les maps String → FuelType (ignorer les clés inconnues)
        val customPricesByType = request.customPrices.mapNotNull { (key, value) ->
            runCatching { FuelType.fromString(key) to value }.getOrNull()
        }.toMap()

        val customConsumptionsByType = request.customConsumptions.mapNotNull { (key, value) ->
            runCatching { FuelType.fromString(key) to value }.getOrNull()
        }.toMap()

        val costBreakdown = simulationEngine.buildCostBreakdown(
            distanceKm = route.distanceKm,
            selectedFuelType = fuelType,
            trafficFactor = resolvedTrafficFactor,
            baseDurationMinutes = route.durationMinutes,
            customPrices = customPricesByType,
            customConsumptions = customConsumptionsByType,
        )

        val simulation = Simulation.create(
            userId = request.userId,
            vehicleId = request.vehicleId,
            fromCoordinates = from,
            fromLabel = request.fromLabel,
            toCoordinates = to,
            toLabel = request.toLabel,
            route = route,
            trafficMode = request.trafficMode,
            trafficFactor = resolvedTrafficFactor,
            departureHour = request.departureHour,
            departureDay = request.departureDay,
            fuelType = fuelType,
            costBreakdown = costBreakdown,
        )

        val saved = if (request.userId != null) {
            simulationRepository.save(simulation)
        } else {
            simulation
        }

        if (request.saveToHistory && request.userId != null) {
            val history = JourneyHistory(
                id = UUID.randomUUID().toString(),
                userId = request.userId,
                simulationId = saved.id,
                fuelType = fuelType.name,
                distanceKm = route.distanceKm,
                costTotal = costBreakdown.costTotal,
                durationMinutes = costBreakdown.durationAdjustedMinutes,
                carbonEmissionKg = route.distanceKm * 0.120,
                createdAt = Clock.System.now().toString(),
                tags = emptyList(),
            )
            historyRepository.save(history)
        }

        return saved
    }
}
