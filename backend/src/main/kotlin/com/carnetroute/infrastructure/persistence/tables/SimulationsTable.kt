package com.carnetroute.infrastructure.persistence.tables

import org.jetbrains.exposed.sql.Table

object SimulationsTable : Table("simulations") {
    val id = varchar("id", 36)
    val userId = varchar("user_id", 36).nullable().index()
    val vehicleId = varchar("vehicle_id", 36).nullable()
    val fromLat = double("from_lat")
    val fromLng = double("from_lng")
    val fromLabel = varchar("from_label", 500)
    val toLat = double("to_lat")
    val toLng = double("to_lng")
    val toLabel = varchar("to_label", 500)
    val distanceKm = double("distance_km")
    val durationMinutes = double("duration_minutes")
    val geometry = text("geometry").nullable() // JSON array
    val trafficMode = varchar("traffic_mode", 20)
    val trafficFactor = double("traffic_factor")
    val fuelType = varchar("fuel_type", 50)
    val pricePerUnit = double("price_per_unit")
    val consumptionPer100km = double("consumption_per_100km")
    val fuelConsumedTotal = double("fuel_consumed_total")
    val costTotal = double("cost_total")
    val durationAdjustedMinutes = double("duration_adjusted_minutes")
    val comparisonJson = text("comparison_json") // JSON
    val createdAt = varchar("created_at", 30)
    override val primaryKey = PrimaryKey(id)
}
