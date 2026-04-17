package com.carnetroute.domain.simulation.vo

import kotlinx.serialization.Serializable

@Serializable
data class ComparisonEntry(
    val fuelType: String,
    val pricePerUnit: Double,
    val consumptionPer100km: Double,
    val fuelConsumed: Double,
    val totalCost: Double,
    val unit: String
)

@Serializable
data class CostBreakdown(
    val fuelType: String,
    val pricePerUnit: Double,
    val consumptionPer100km: Double,
    val fuelConsumedTotal: Double,
    val costTotal: Double,
    val durationAdjustedMinutes: Double,
    val comparison: Map<String, ComparisonEntry>
)
