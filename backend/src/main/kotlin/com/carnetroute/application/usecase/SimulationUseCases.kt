package com.carnetroute.application.usecase

import com.carnetroute.domain.model.*
import com.carnetroute.domain.port.GeocodingPort
import com.carnetroute.domain.port.RoutingPort
import com.carnetroute.domain.service.SimulationEngine

/** Agrégat des use cases pour l'injection dans les routes Ktor. */
class SimulationUseCases(
    val simulate: SimulateRouteUseCase,
    val heatmap: GenerateHeatmapUseCase,
    val fuels: GetFuelProfilesUseCase,
    val geocode: GeocodeUseCase,
    val reverseGeocode: ReverseGeocodeUseCase
)

/** Exécute la simulation complète d'un trajet. */
class SimulateRouteUseCase(
    private val routingPort: RoutingPort,
    private val engine: SimulationEngine
) {
    suspend fun execute(request: SimulationRequest): SimulationResult {
        val route = routingPort.getRoute(request.from, request.to, request.avoidTolls)
        return engine.simulate(request, route)
    }
}

/** Génère la heatmap hebdomadaire (7j × 24h) pour un trajet. */
class GenerateHeatmapUseCase(
    private val routingPort: RoutingPort,
    private val engine: SimulationEngine
) {
    suspend fun execute(request: SimulationRequest): WeeklyHeatmapResult {
        val route = routingPort.getRoute(request.from, request.to, request.avoidTolls)
        return engine.generateHeatmapGrid(
            route             = route,
            fuelType          = request.fuelType,
            customPrice       = request.customPrice,
            customConsumption = request.customConsumption
        )
    }
}

/** Retourne la liste des profils carburant disponibles. */
class GetFuelProfilesUseCase(private val engine: SimulationEngine) {
    fun execute(): Collection<FuelProfile> = SimulationEngine.FUEL_PROFILES.values
}

/** Proxy autocomplétion d'adresses (API Gouv). */
class GeocodeUseCase(private val geocodingPort: GeocodingPort) {
    suspend fun execute(query: String, limit: Int = 7): String =
        geocodingPort.autocomplete(query, limit)
}

/** Proxy géocodage inverse (API Gouv). */
class ReverseGeocodeUseCase(private val geocodingPort: GeocodingPort) {
    suspend fun execute(lat: Double, lng: Double): String =
        geocodingPort.reverse(lat, lng)
}
