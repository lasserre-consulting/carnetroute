package com.carnetroute.application.vehicle

import com.carnetroute.domain.vehicle.Vehicle
import com.carnetroute.domain.vehicle.VehicleRepository

class GetVehiclesUseCase(
    private val vehicleRepository: VehicleRepository
) {
    suspend fun execute(userId: String): List<Vehicle> {
        return vehicleRepository.findByUserId(userId)
    }
}
