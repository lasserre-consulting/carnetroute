package com.carnetroute.application.vehicle

import com.carnetroute.domain.shared.DomainException
import com.carnetroute.domain.vehicle.Vehicle
import com.carnetroute.domain.vehicle.VehicleRepository
import com.carnetroute.domain.vehicle.vo.FuelProfile
import com.carnetroute.domain.vehicle.vo.FuelType

data class CreateVehicleRequest(
    val userId: String,
    val name: String,
    val fuelType: String,
    val consumptionPer100km: Double? = null,
    val costPerUnit: Double? = null,
    val tankCapacity: Double? = null,
    val yearMake: Int? = null,
    val isDefault: Boolean = false,
)

class CreateVehicleUseCase(
    private val vehicleRepository: VehicleRepository,
) {
    suspend fun execute(request: CreateVehicleRequest): Vehicle {
        if (request.name.isBlank()) {
            throw DomainException.ValidationError("name", "Vehicle name is required")
        }

        val fuelType = FuelType.fromString(request.fuelType)

        val consumption = request.consumptionPer100km ?: fuelType.defaultConsumptionPer100km
        if (consumption <= 0) {
            throw DomainException.ValidationError("consumptionPer100km", "Consumption must be greater than 0")
        }

        val costPerUnit = request.costPerUnit ?: fuelType.defaultPricePerUnit
        val tankCapacity = request.tankCapacity ?: 50.0

        val fuelProfile = FuelProfile(
            fuelType = fuelType.name,
            consumptionPer100km = consumption,
            costPerUnit = costPerUnit,
        )

        val vehicle = Vehicle.create(
            userId = request.userId,
            name = request.name,
            fuelProfile = fuelProfile,
            tankCapacity = tankCapacity,
            yearMake = request.yearMake ?: 2020,
            isDefault = request.isDefault,
        )

        val saved = vehicleRepository.save(vehicle)

        if (request.isDefault) {
            vehicleRepository.setDefault(request.userId, saved.id)
        }

        return saved
    }
}
