package com.carnetroute.infrastructure.persistence.repositories

import com.carnetroute.domain.fuelprice.FuelPrice
import com.carnetroute.domain.fuelprice.FuelPriceRepository
import com.carnetroute.infrastructure.persistence.DatabaseFactory
import com.carnetroute.infrastructure.persistence.dbQuery
import com.carnetroute.infrastructure.persistence.tables.FuelPricesTable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update

class FuelPriceRepositoryImpl(
    @Suppress("UNUSED_PARAMETER") private val databaseFactory: DatabaseFactory,
) : FuelPriceRepository {

    override suspend fun findAll(): List<FuelPrice> = dbQuery {
        FuelPricesTable.selectAll()
            .map { it.toFuelPrice() }
    }

    override suspend fun findByFuelType(fuelType: String): FuelPrice? = dbQuery {
        FuelPricesTable.selectAll()
            .where { FuelPricesTable.fuelType eq fuelType }
            .singleOrNull()
            ?.toFuelPrice()
    }

    override suspend fun save(fuelPrice: FuelPrice): FuelPrice = dbQuery {
        val existing = FuelPricesTable.selectAll()
            .where { FuelPricesTable.fuelType eq fuelPrice.fuelType }
            .singleOrNull()

        if (existing != null) {
            FuelPricesTable.update({ FuelPricesTable.fuelType eq fuelPrice.fuelType }) {
                it[pricePerUnit] = fuelPrice.pricePerUnit
                it[currency] = fuelPrice.currency
                it[unit] = fuelPrice.unit
                it[sourceRef] = fuelPrice.source
                it[updatedAt] = fuelPrice.updatedAt
            }
        } else {
            FuelPricesTable.insert {
                it[fuelType] = fuelPrice.fuelType
                it[pricePerUnit] = fuelPrice.pricePerUnit
                it[currency] = fuelPrice.currency
                it[unit] = fuelPrice.unit
                it[sourceRef] = fuelPrice.source
                it[updatedAt] = fuelPrice.updatedAt
            }
        }
        fuelPrice
    }

    override suspend fun saveAll(fuelPrices: List<FuelPrice>) {
        fuelPrices.forEach { save(it) }
    }

    private fun ResultRow.toFuelPrice(): FuelPrice = FuelPrice(
        fuelType = this[FuelPricesTable.fuelType],
        pricePerUnit = this[FuelPricesTable.pricePerUnit],
        currency = this[FuelPricesTable.currency],
        unit = this[FuelPricesTable.unit],
        source = this[FuelPricesTable.sourceRef],
        updatedAt = this[FuelPricesTable.updatedAt]
    )
}
