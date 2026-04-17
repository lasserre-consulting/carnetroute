package com.carnetroute.plugins

import com.carnetroute.application.fuelprice.GetFuelPricesUseCase
import com.carnetroute.application.geocoding.AutocompleteUseCase
import com.carnetroute.application.history.GetJourneyHistoryUseCase
import com.carnetroute.application.history.GetUserStatisticsUseCase
import com.carnetroute.application.simulation.GenerateHeatmapUseCase
import com.carnetroute.application.simulation.GetSimulationHistoryUseCase
import com.carnetroute.application.simulation.SimulateRouteUseCase
import com.carnetroute.application.user.AuthenticateUserUseCase
import com.carnetroute.application.user.CreateUserUseCase
import com.carnetroute.application.user.GetUserProfileUseCase
import com.carnetroute.application.user.UpdateUserProfileUseCase
import com.carnetroute.application.vehicle.CreateVehicleUseCase
import com.carnetroute.application.vehicle.DeleteVehicleUseCase
import com.carnetroute.application.vehicle.GetVehiclesUseCase
import com.carnetroute.application.vehicle.UpdateVehicleUseCase
import com.carnetroute.interfaces.http.configureAuthRoutes
import com.carnetroute.interfaces.http.configureFuelPriceRoutes
import com.carnetroute.interfaces.http.configureHistoryRoutes
import com.carnetroute.interfaces.http.configureSimulationRoutes
import com.carnetroute.interfaces.http.configureVehicleRoutes
import com.carnetroute.interfaces.http.configureWebSocketRoutes
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import org.koin.ktor.ext.inject

fun Application.configureRouting() {
    // Inject all use cases at Application level (works fine — no RoutingKt issue)
    val createUserUseCase: CreateUserUseCase by inject()
    val authenticateUserUseCase: AuthenticateUserUseCase by inject()
    val getUserProfileUseCase: GetUserProfileUseCase by inject()
    val updateUserProfileUseCase: UpdateUserProfileUseCase by inject()

    val getVehiclesUseCase: GetVehiclesUseCase by inject()
    val createVehicleUseCase: CreateVehicleUseCase by inject()
    val updateVehicleUseCase: UpdateVehicleUseCase by inject()
    val deleteVehicleUseCase: DeleteVehicleUseCase by inject()

    val simulateRouteUseCase: SimulateRouteUseCase by inject()
    val generateHeatmapUseCase: GenerateHeatmapUseCase by inject()
    val getSimulationHistoryUseCase: GetSimulationHistoryUseCase by inject()
    val autocompleteUseCase: AutocompleteUseCase by inject()

    val getFuelPricesUseCase: GetFuelPricesUseCase by inject()

    val getJourneyHistoryUseCase: GetJourneyHistoryUseCase by inject()
    val getUserStatisticsUseCase: GetUserStatisticsUseCase by inject()

    routing {
        route("/api") {
            configureAuthRoutes(createUserUseCase, authenticateUserUseCase, getUserProfileUseCase, updateUserProfileUseCase)
            configureVehicleRoutes(getVehiclesUseCase, createVehicleUseCase, updateVehicleUseCase, deleteVehicleUseCase)
            configureSimulationRoutes(simulateRouteUseCase, generateHeatmapUseCase, getSimulationHistoryUseCase, autocompleteUseCase)
            configureFuelPriceRoutes(getFuelPricesUseCase)
            configureHistoryRoutes(getJourneyHistoryUseCase, getUserStatisticsUseCase)

            get("/health") {
                call.respond(
                    HttpStatusCode.OK,
                    mapOf("status" to "ok", "version" to "2.0.0")
                )
            }
        }

        configureWebSocketRoutes()
    }
}
