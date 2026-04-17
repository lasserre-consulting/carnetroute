package com.carnetroute.domain.simulation

import com.carnetroute.domain.simulation.vo.ComparisonEntry
import com.carnetroute.domain.simulation.vo.CostBreakdown
import com.carnetroute.domain.vehicle.vo.FuelType
import kotlin.math.*

class SimulationEngine {

    // Haversine formula
    fun calculateDistance(from: com.carnetroute.domain.simulation.vo.Coordinates, to: com.carnetroute.domain.simulation.vo.Coordinates): Double {
        val R = 6371.0
        val dLat = Math.toRadians(to.lat - from.lat)
        val dLng = Math.toRadians(to.lng - from.lng)
        val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(from.lat)) * cos(Math.toRadians(to.lat)) * sin(dLng / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return R * c * 1.15 // road factor
    }

    // Traffic factor based on day (1=Mon..7=Sun) and hour (0-23)
    fun calculateTrafficFactor(dayOfWeek: Int, hour: Int): Double {
        // Peak hours: weekdays 7-9h and 17-19h -> factor 1.6-2.0
        // Off-peak: nights, weekends -> factor 1.0-1.2
        // Friday evening: 16-19h -> factor 1.8-2.0
        val isWeekend = dayOfWeek >= 6
        return when {
            isWeekend && hour in 0..6 -> 1.0
            isWeekend && hour in 7..9 -> 1.2
            isWeekend && hour in 10..17 -> 1.3
            isWeekend && hour in 18..23 -> 1.1
            dayOfWeek == 5 && hour in 16..19 -> 1.9 // Friday evening rush
            !isWeekend && hour in 7..9 -> 1.7   // Morning rush
            !isWeekend && hour in 17..19 -> 1.8  // Evening rush
            !isWeekend && hour in 0..5 -> 1.0
            !isWeekend && hour in 6..6 -> 1.2
            !isWeekend && hour in 10..16 -> 1.3
            else -> 1.1
        }
    }

    // Calculate cost for one fuel type
    fun calculateCost(
        distanceKm: Double,
        fuelType: FuelType,
        customConsumption: Double? = null,
        customPrice: Double? = null
    ): ComparisonEntry {
        val consumption = customConsumption ?: fuelType.defaultConsumptionPer100km
        val price = customPrice ?: fuelType.defaultPricePerUnit
        val fuelConsumed = (distanceKm / 100.0) * consumption
        val totalCost = fuelConsumed * price
        return ComparisonEntry(
            fuelType = fuelType.name,
            pricePerUnit = price,
            consumptionPer100km = consumption,
            fuelConsumed = fuelConsumed,
            totalCost = totalCost,
            unit = fuelType.unit
        )
    }

    // Generate full cost breakdown with comparison across all fuel types
    fun buildCostBreakdown(
        distanceKm: Double,
        selectedFuelType: FuelType,
        trafficFactor: Double,
        baseDurationMinutes: Double,
        customPrices: Map<FuelType, Double> = emptyMap(),
        customConsumptions: Map<FuelType, Double> = emptyMap()
    ): CostBreakdown {
        // Stop-and-go traffic increases fuel consumption:
        //   factor 1.0 (fluide)        → +0%  consumption
        //   factor 1.5 (chargé)        → +25% consumption
        //   factor 2.0 (embouteillages) → +50% consumption
        // Electric vehicles benefit from regenerative braking → penalty halved
        val thermalMultiplier = 1.0 + (trafficFactor - 1.0) * 0.5
        val electricMultiplier = 1.0 + (trafficFactor - 1.0) * 0.2

        fun multiplierFor(ft: FuelType) = if (ft == FuelType.ELECTRIC) electricMultiplier else thermalMultiplier

        val mainEntry = calculateCost(
            distanceKm,
            selectedFuelType,
            (customConsumptions[selectedFuelType] ?: selectedFuelType.defaultConsumptionPer100km) * multiplierFor(selectedFuelType),
            customPrices[selectedFuelType]
        )
        val comparison = FuelType.entries.associate { ft ->
            ft.name to calculateCost(
                distanceKm,
                ft,
                (customConsumptions[ft] ?: ft.defaultConsumptionPer100km) * multiplierFor(ft),
                customPrices[ft]
            )
        }
        return CostBreakdown(
            fuelType = selectedFuelType.name,
            pricePerUnit = mainEntry.pricePerUnit,
            consumptionPer100km = mainEntry.consumptionPer100km,
            fuelConsumedTotal = mainEntry.fuelConsumed,
            costTotal = mainEntry.totalCost,
            durationAdjustedMinutes = baseDurationMinutes * trafficFactor,
            comparison = comparison
        )
    }

    // Generate heatmap: 7 days x 24 hours traffic factors
    fun generateHeatmap(): Array<DoubleArray> {
        return Array(7) { day -> DoubleArray(24) { hour -> calculateTrafficFactor(day + 1, hour) } }
    }
}
