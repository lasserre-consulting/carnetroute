package com.carnetroute.domain.fuelprice

interface FuelPriceRepository {
    suspend fun findAll(): List<FuelPrice>
    suspend fun findByFuelType(fuelType: String): FuelPrice?
    suspend fun save(fuelPrice: FuelPrice): FuelPrice
    suspend fun saveAll(fuelPrices: List<FuelPrice>)
}
