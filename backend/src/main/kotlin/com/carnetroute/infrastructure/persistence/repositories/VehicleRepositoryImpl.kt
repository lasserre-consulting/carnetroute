package com.carnetroute.infrastructure.persistence.repositories

import com.carnetroute.domain.vehicle.Vehicle
import com.carnetroute.domain.vehicle.VehicleRepository
import com.carnetroute.domain.vehicle.vo.FuelProfile
import com.carnetroute.infrastructure.persistence.DatabaseFactory
import com.carnetroute.infrastructure.persistence.dbQuery
import com.carnetroute.infrastructure.persistence.tables.VehiclesTable
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq

class VehicleRepositoryImpl(
    @Suppress("UNUSED_PARAMETER") private val databaseFactory: DatabaseFactory,
) : VehicleRepository {

    override suspend fun findById(id: String): Vehicle? = dbQuery {
        VehiclesTable.selectAll()
            .where { VehiclesTable.id eq id }
            .singleOrNull()
            ?.toVehicle()
    }

    override suspend fun findByUserId(userId: String): List<Vehicle> = dbQuery {
        VehiclesTable.selectAll()
            .where { VehiclesTable.userId eq userId }
            .orderBy(VehiclesTable.createdAt, SortOrder.DESC)
            .map { it.toVehicle() }
    }

    override suspend fun save(vehicle: Vehicle): Vehicle = dbQuery {
        VehiclesTable.insert {
            it[id] = vehicle.id
            it[userId] = vehicle.userId
            it[name] = vehicle.name
            it[fuelType] = vehicle.fuelProfile.fuelType
            it[consumptionPer100km] = vehicle.fuelProfile.consumptionPer100km
            it[costPerUnit] = vehicle.fuelProfile.costPerUnit
            it[tankCapacity] = vehicle.tankCapacity
            it[emissionsGCO2PerKm] = vehicle.emissionsGCO2PerKm
            it[yearMake] = vehicle.yearMake
            it[isDefault] = vehicle.isDefault
            it[createdAt] = vehicle.createdAt
            it[updatedAt] = vehicle.updatedAt
        }
        vehicle
    }

    override suspend fun update(vehicle: Vehicle): Vehicle = dbQuery {
        VehiclesTable.update({ VehiclesTable.id eq vehicle.id }) {
            it[name] = vehicle.name
            it[fuelType] = vehicle.fuelProfile.fuelType
            it[consumptionPer100km] = vehicle.fuelProfile.consumptionPer100km
            it[costPerUnit] = vehicle.fuelProfile.costPerUnit
            it[tankCapacity] = vehicle.tankCapacity
            it[emissionsGCO2PerKm] = vehicle.emissionsGCO2PerKm
            it[yearMake] = vehicle.yearMake
            it[isDefault] = vehicle.isDefault
            it[updatedAt] = Clock.System.now().toString()
        }
        vehicle
    }

    override suspend fun delete(id: String): Unit = dbQuery {
        VehiclesTable.deleteWhere { VehiclesTable.id eq id }
    }

    override suspend fun setDefault(vehicleId: String, userId: String): Unit = dbQuery {
        // Reset tous les véhicules de l'utilisateur
        VehiclesTable.update({ VehiclesTable.userId eq userId }) {
            it[isDefault] = false
        }
        // Marque le véhicule cible comme défaut
        VehiclesTable.update({ (VehiclesTable.id eq vehicleId) and (VehiclesTable.userId eq userId) }) {
            it[isDefault] = true
        }
    }

    private fun ResultRow.toVehicle(): Vehicle = Vehicle(
        id = this[VehiclesTable.id],
        userId = this[VehiclesTable.userId],
        name = this[VehiclesTable.name],
        fuelProfile = FuelProfile(
            fuelType = this[VehiclesTable.fuelType],
            consumptionPer100km = this[VehiclesTable.consumptionPer100km],
            costPerUnit = this[VehiclesTable.costPerUnit],
        ),
        tankCapacity = this[VehiclesTable.tankCapacity],
        emissionsGCO2PerKm = this[VehiclesTable.emissionsGCO2PerKm],
        yearMake = this[VehiclesTable.yearMake],
        isDefault = this[VehiclesTable.isDefault],
        createdAt = this[VehiclesTable.createdAt],
        updatedAt = this[VehiclesTable.updatedAt],
    )
}
