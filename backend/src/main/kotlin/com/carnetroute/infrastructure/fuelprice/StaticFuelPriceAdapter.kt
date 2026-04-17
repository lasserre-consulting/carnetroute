package com.carnetroute.infrastructure.fuelprice

import com.carnetroute.domain.fuelprice.FuelPricePort
import java.time.Instant

class StaticFuelPriceAdapter : FuelPricePort {

    // Prix de base avril 2026 (EUR/L sauf ELECTRIC en EUR/kWh)
    private val defaultPrices = mapOf(
        "SP95" to 1.85,
        "SP98" to 1.96,
        "DIESEL" to 2.19,
        "E85" to 0.73,
        "GPL" to 1.05,
        "ELECTRIC" to 0.44
    )

    override suspend fun fetchLatestPrices(): Map<String, Double> = defaultPrices
}
