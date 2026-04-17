package com.carnetroute.application.fuelprice

import com.carnetroute.domain.fuelprice.FuelPrice
import com.carnetroute.domain.fuelprice.FuelPriceRepository
import com.carnetroute.domain.vehicle.vo.FuelType
import kotlinx.datetime.Clock

class GetFuelPricesUseCase(
    private val fuelPriceRepository: FuelPriceRepository
) {
    suspend fun execute(): List<FuelPrice> {
        val prices = fuelPriceRepository.findAll()
        if (prices.isNotEmpty()) return prices

        val now = Clock.System.now().toString()
        return FuelType.entries.map { fuelType ->
            FuelPrice(
                fuelType = fuelType.name,
                pricePerUnit = fuelType.defaultPricePerUnit,
                currency = "EUR",
                unit = fuelType.unit,
                source = "static",
                updatedAt = now,
            )
        }
    }
}
