package com.carnetroute.domain.vehicle

interface VehicleRepository {
    suspend fun findByUserId(userId: String): List<Vehicle>
    suspend fun findById(id: String): Vehicle?
    suspend fun save(vehicle: Vehicle): Vehicle
    suspend fun update(vehicle: Vehicle): Vehicle
    suspend fun delete(id: String)
    suspend fun setDefault(vehicleId: String, userId: String)
}
