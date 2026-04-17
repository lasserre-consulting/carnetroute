package com.carnetroute.application.vehicle

import com.carnetroute.domain.shared.DomainException
import com.carnetroute.domain.vehicle.VehicleRepository

class DeleteVehicleUseCase(
    private val vehicleRepository: VehicleRepository
) {
    suspend fun execute(vehicleId: String, userId: String) {
        val vehicle = vehicleRepository.findById(vehicleId)
            ?: throw DomainException.VehicleNotFound(
                runCatching { java.util.UUID.fromString(vehicleId) }.getOrElse { java.util.UUID.nameUUIDFromBytes(vehicleId.toByteArray()) }
            )

        if (vehicle.userId != userId) {
            throw DomainException.Unauthorized("You do not own this vehicle")
        }

        vehicleRepository.delete(vehicleId)
    }
}
