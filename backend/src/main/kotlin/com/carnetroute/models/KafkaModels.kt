package com.carnetroute.models

import kotlinx.serialization.Serializable

@Serializable
data class FuelPriceEvent(
    val fuelType: String,
    val price: Double,
    val previousPrice: Double,
    val changePercent: Double,
    val timestamp: Long,
    val source: String = "market-feed"
)

@Serializable
data class FuelPriceAlert(
    val id: String,
    val fuelType: String,
    val fuelLabel: String,
    val fuelIcon: String,
    val currentPrice: Double,
    val previousPrice: Double,
    val changePercent: Double,
    val direction: String,   // "up" | "down"
    val severity: String,    // "info" | "warning" | "critical"
    val message: String,
    val timestamp: Long
)

@Serializable
data class LiveFuelPrices(
    val prices: Map<String, Double>,
    val lastUpdate: Long,
    val alerts: List<FuelPriceAlert> = emptyList()
)
