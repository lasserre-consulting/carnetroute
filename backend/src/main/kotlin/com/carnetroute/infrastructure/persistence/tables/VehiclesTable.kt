package com.carnetroute.infrastructure.persistence.tables

import org.jetbrains.exposed.sql.Table

object VehiclesTable : Table("vehicles") {
    val id = varchar("id", 36)
    val userId = varchar("user_id", 36).index()
    val name = varchar("name", 255)
    val fuelType = varchar("fuel_type", 50)
    val consumptionPer100km = double("consumption_per_100km")
    val costPerUnit = double("cost_per_unit")
    val tankCapacity = double("tank_capacity")
    val emissionsGCO2PerKm = double("emissions_gco2_per_km")
    val yearMake = integer("year_make")
    val isDefault = bool("is_default").default(false)
    val createdAt = varchar("created_at", 30)
    val updatedAt = varchar("updated_at", 30)
    override val primaryKey = PrimaryKey(id)
}
