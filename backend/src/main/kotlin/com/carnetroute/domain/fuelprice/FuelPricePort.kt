package com.carnetroute.domain.fuelprice

interface FuelPricePort {
    suspend fun fetchLatestPrices(): Map<String, Double>
}
