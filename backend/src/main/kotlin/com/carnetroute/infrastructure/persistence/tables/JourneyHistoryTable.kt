package com.carnetroute.infrastructure.persistence.tables

import org.jetbrains.exposed.sql.Table

// Alias for backward compatibility
typealias JourneyHistoryTable = JourneyHistoriesTable

object JourneyHistoriesTable : Table("journey_history") {
    val id = varchar("id", 36)
    val userId = varchar("user_id", 36).index()
    val simulationId = varchar("simulation_id", 36)
    val fuelType = varchar("fuel_type", 50)
    val distanceKm = double("distance_km")
    val costTotal = double("cost_total")
    val durationMinutes = double("duration_minutes")
    val carbonEmissionKg = double("carbon_emission_kg")
    val tags = text("tags").default("[]") // JSON array
    val createdAt = varchar("created_at", 30).index()
    override val primaryKey = PrimaryKey(id)
}
