package com.carnetroute.interfaces.http

import com.carnetroute.application.vehicle.CreateVehicleUseCase
import com.carnetroute.application.vehicle.CreateVehicleRequest as DomainCreateVehicleRequest
import com.carnetroute.application.vehicle.DeleteVehicleUseCase
import com.carnetroute.application.vehicle.GetVehiclesUseCase
import com.carnetroute.application.vehicle.UpdateVehicleUseCase
import com.carnetroute.application.vehicle.UpdateVehicleRequest as DomainUpdateVehicleRequest
import com.carnetroute.domain.vehicle.Vehicle
import com.carnetroute.interfaces.http.dto.CreateVehicleRequest
import com.carnetroute.interfaces.http.dto.UpdateVehicleRequest
import com.carnetroute.interfaces.http.dto.VehicleResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
fun Route.configureVehicleRoutes(
    getVehiclesUseCase: GetVehiclesUseCase,
    createVehicleUseCase: CreateVehicleUseCase,
    updateVehicleUseCase: UpdateVehicleUseCase,
    deleteVehicleUseCase: DeleteVehicleUseCase,
) {

    route("/vehicles") {
        authenticate("auth-jwt") {
            // GET /api/vehicles
            get {
                val userId = call.principal<JWTPrincipal>()!!.payload.getClaim("userId").asString()
                val vehicles = getVehiclesUseCase.execute(userId)
                call.respond(vehicles.map { it.toResponse() })
            }

            // POST /api/vehicles
            post {
                val userId = call.principal<JWTPrincipal>()!!.payload.getClaim("userId").asString()
                val request = call.receive<CreateVehicleRequest>()
                val vehicle = createVehicleUseCase.execute(
                    DomainCreateVehicleRequest(
                        userId = userId,
                        name = request.name,
                        fuelType = request.fuelType,
                        consumptionPer100km = request.consumptionPer100km,
                        costPerUnit = request.costPerUnit,
                        tankCapacity = request.tankCapacity,
                        yearMake = request.yearMake,
                        isDefault = request.isDefault
                    )
                )
                call.respond(HttpStatusCode.Created, vehicle.toResponse())
            }

            // PUT /api/vehicles/{id}
            put("/{id}") {
                val userId = call.principal<JWTPrincipal>()!!.payload.getClaim("userId").asString()
                val vehicleId = call.parameters["id"]
                    ?: return@put call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing vehicle id"))
                val request = call.receive<UpdateVehicleRequest>()
                val vehicle = updateVehicleUseCase.execute(
                    vehicleId = vehicleId,
                    userId = userId,
                    request = DomainUpdateVehicleRequest(
                        name = request.name,
                        consumptionPer100km = request.consumptionPer100km,
                        costPerUnit = request.costPerUnit,
                        isDefault = request.isDefault
                    )
                )
                call.respond(vehicle.toResponse())
            }

            // DELETE /api/vehicles/{id}
            delete("/{id}") {
                val userId = call.principal<JWTPrincipal>()!!.payload.getClaim("userId").asString()
                val vehicleId = call.parameters["id"]
                    ?: return@delete call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing vehicle id"))
                deleteVehicleUseCase.execute(vehicleId = vehicleId, userId = userId)
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}

private fun Vehicle.toResponse(): VehicleResponse = VehicleResponse(
    id = id,
    userId = userId,
    name = name,
    fuelType = fuelProfile.fuelType,
    consumptionPer100km = fuelProfile.consumptionPer100km,
    costPerUnit = fuelProfile.costPerUnit,
    tankCapacity = tankCapacity,
    yearMake = yearMake,
    isDefault = isDefault,
    createdAt = createdAt
)
