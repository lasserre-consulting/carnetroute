package com.carnetroute.infrastructure.persistence.tables

import org.jetbrains.exposed.sql.Table

object FuelPricesTable : Table("fuel_prices") {
    val fuelType = varchar("fuel_type", 50)
    val pricePerUnit = double("price_per_unit")
    val currency = varchar("currency", 10).default("EUR")
    val unit = varchar("unit", 10)
    val sourceRef = varchar("source", 100)
    val updatedAt = varchar("updated_at", 30)
    override val primaryKey = PrimaryKey(fuelType)
}
