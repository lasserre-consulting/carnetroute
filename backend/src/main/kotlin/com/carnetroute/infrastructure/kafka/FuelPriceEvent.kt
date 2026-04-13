package com.carnetroute.infrastructure.kafka

import kotlinx.serialization.Serializable

/** Événement Kafka interne — fluctuation de prix sur le topic fuel.prices. */
@Serializable
data class FuelPriceEvent(
    val fuelType: String,
    val price: Double,
    val previousPrice: Double,
    val changePercent: Double,
    val timestamp: Long,
    val source: String = "market-feed"
)
