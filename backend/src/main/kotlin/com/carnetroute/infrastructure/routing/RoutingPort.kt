package com.carnetroute.infrastructure.routing

import com.carnetroute.domain.simulation.vo.Coordinates
import com.carnetroute.domain.simulation.vo.Route

interface RoutingPort {
    suspend fun getRoute(from: Coordinates, to: Coordinates): Route
}
