package com.carnetroute.domain.port

import com.carnetroute.domain.model.Coordinates
import com.carnetroute.domain.model.RouteInfo

interface RoutingPort {
    suspend fun getRoute(from: Coordinates, to: Coordinates, avoidTolls: Boolean): RouteInfo
}
