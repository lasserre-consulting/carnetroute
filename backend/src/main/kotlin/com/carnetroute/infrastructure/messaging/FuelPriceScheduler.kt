package com.carnetroute.infrastructure.messaging

import com.carnetroute.domain.fuelprice.FuelPrice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.time.Instant
import kotlin.random.Random

class FuelPriceScheduler(
    private val producer: FuelPriceProducer,
    private val intervalMs: Long = 30_000L
) {
    private val logger = LoggerFactory.getLogger(FuelPriceScheduler::class.java)
    private val scope = CoroutineScope(Dispatchers.Default)
    private var job: Job? = null

    // Prix de base avril 2026 (EUR)
    private val basePrices = mapOf(
        "SP95" to 1.85,
        "SP98" to 1.96,
        "DIESEL" to 2.19,
        "E85" to 0.73,
        "GPL" to 1.05,
        "ELECTRIC" to 0.44
    )

    fun start() {
        job = scope.launch {
            logger.info("FuelPriceScheduler started — interval: ${intervalMs}ms")
            while (isActive) {
                try {
                    val prices = generatePrices()
                    producer.publish(prices)
                    logger.info("Published ${prices.size} simulated fuel prices")
                } catch (e: Exception) {
                    logger.error("Error publishing simulated fuel prices", e)
                }
                delay(intervalMs)
            }
        }
    }

    fun stop() {
        job?.cancel()
        logger.info("FuelPriceScheduler stopped")
    }

    private fun generatePrices(): List<FuelPrice> {
        val now = Instant.now().toString()
        return basePrices.map { (fuelType, basePrice) ->
            // Variation aléatoire ±2%
            val variation = 1.0 + (Random.nextDouble() * 0.04 - 0.02)
            val price = (basePrice * variation * 1000).toLong() / 1000.0
            FuelPrice(
                fuelType = fuelType,
                pricePerUnit = price,
                currency = "EUR",
                unit = if (fuelType == "ELECTRIC") "kWh" else "L",
                source = "simulated",
                updatedAt = now
            )
        }
    }
}
