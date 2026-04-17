package com.carnetroute.application.vehicle

import com.carnetroute.domain.shared.DomainException
import com.carnetroute.domain.vehicle.Vehicle
import com.carnetroute.domain.vehicle.VehicleRepository
import com.carnetroute.domain.vehicle.vo.FuelProfile
import com.carnetroute.domain.vehicle.vo.FuelType
import kotlinx.datetime.Clock

data class UpdateVehicleRequest(
    val name: String? = null,
    val fuelType: String? = null,
    val consumptionPer100km: Double? = null,
    val costPerUnit: Double? = null,
    val tankCapacity: Double? = null,
    val yearMake: Int? = null,
    val isDefault: Boolean? = null,
)

class UpdateVehicleUseCase(
    private val vehicleRepository: VehicleRepository,
) {
    suspend fun execute(vehicleId: String, userId: String, request: UpdateVehicleRequest): Vehicle {
        val existing = vehicleRepository.findById(vehicleId)
            ?: throw DomainException.VehicleNotFound(
                runCatching { java.util.UUID.fromString(vehicleId) }
                    .getOrElse { java.util.UUID.nameUUIDFromBytes(vehicleId.toByteArray()) }
            )

        if (existing.userId != userId) {
            throw DomainException.Unauthorized("You do not own this vehicle")
        }

        val resolvedFuelType = request.fuelType?.let { FuelType.fromString(it) }

        val consumption = request.consumptionPer100km
        if (consumption != null && consumption <= 0) {
            throw DomainException.ValidationError("consumptionPer100km", "Consumption must be greater than 0")
        }

        val updatedProfile = FuelProfile(
            fuelType = resolvedFuelType?.name ?: existing.fuelProfile.fuelType,
            consumptionPer100km = consumption ?: existing.fuelProfile.consumptionPer100km,
            costPerUnit = request.costPerUnit ?: existing.fuelProfile.costPerUnit,
        )

        val updated = existing.copy(
            name = request.name ?: existing.name,
            fuelProfile = updatedProfile,
            tankCapacity = request.tankCapacity ?: existing.tankCapacity,
            yearMake = request.yearMake ?: existing.yearMake,
            isDefault = request.isDefault ?: existing.isDefault,
            updatedAt = Clock.System.now().toString(),
        )

        val saved = vehicleRepository.update(updated)

        if (request.isDefault == true) {
            vehicleRepository.setDefault(userId, vehicleId)
        }

        return saved
    }
}
