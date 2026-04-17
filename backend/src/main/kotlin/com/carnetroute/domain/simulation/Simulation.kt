package com.carnetroute.domain.simulation

import com.carnetroute.domain.simulation.vo.CostBreakdown
import com.carnetroute.domain.simulation.vo.Coordinates
import com.carnetroute.domain.simulation.vo.Route
import com.carnetroute.domain.simulation.vo.TrafficConditions
import com.carnetroute.domain.vehicle.vo.FuelType
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class Simulation(
    val id: String,
    val userId: String?,
    val vehicleId: String?,
    val route: Route,
    val traffic: TrafficConditions,
    val costs: CostBreakdown,
    val createdAt: String,
) {
    companion object {

        /** Création simple à partir des VOs déjà construits. */
        fun create(
            userId: String?,
            vehicleId: String?,
            route: Route,
            traffic: TrafficConditions,
            costs: CostBreakdown,
        ): Simulation = Simulation(
            id = UUID.randomUUID().toString(),
            userId = userId,
            vehicleId = vehicleId,
            route = route,
            traffic = traffic,
            costs = costs,
            createdAt = Clock.System.now().toString(),
        )

        /**
         * Création à partir des paramètres bruts — construit les VOs Route et
         * TrafficConditions en interne.
         */
        fun create(
            userId: String?,
            vehicleId: String?,
            fromCoordinates: Coordinates,
            fromLabel: String,
            toCoordinates: Coordinates,
            toLabel: String,
            route: Route,
            trafficMode: String,
            trafficFactor: Double,
            departureHour: Int?,
            departureDay: Int?,
            fuelType: FuelType,
            costBreakdown: CostBreakdown,
        ): Simulation {
            val departureTime = if (departureDay != null && departureHour != null) {
                "day=$departureDay hour=$departureHour"
            } else null

            val labeledRoute = route.copy(
                from = fromCoordinates.copy(label = fromLabel),
                to = toCoordinates.copy(label = toLabel),
            )

            return Simulation(
                id = UUID.randomUUID().toString(),
                userId = userId,
                vehicleId = vehicleId,
                route = labeledRoute,
                traffic = TrafficConditions(
                    mode = trafficMode,
                    factor = trafficFactor,
                    departureTime = departureTime,
                ),
                costs = costBreakdown,
                createdAt = Clock.System.now().toString(),
            )
        }
    }
}
