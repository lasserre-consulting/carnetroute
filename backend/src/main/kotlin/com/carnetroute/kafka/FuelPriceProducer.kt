package com.carnetroute.kafka

import com.carnetroute.models.FuelPriceEvent
import com.carnetroute.services.SimulationService
import kotlinx.coroutines.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory
import kotlin.random.Random

/**
 * Produces fuel price updates on Kafka topic [carnetroute.fuel.prices].
 *
 * In production, this would connect to a real market data feed
 * (e.g. from the French government's prix-carburants API).
 * Here it simulates realistic price fluctuations for demo purposes.
 */
class FuelPriceProducer {

    private val logger = LoggerFactory.getLogger(FuelPriceProducer::class.java)
    private val json = Json { encodeDefaults = true }
    private var producer: KafkaProducer<String, String>? = null
    private var job: Job? = null

    // Current simulated prices (start from defaults)
    private val currentPrices = mutableMapOf<String, Double>().apply {
        SimulationService.FUEL_PROFILES.forEach { (key, profile) ->
            put(key, profile.defaultPrice)
        }
    }

    fun start(scope: CoroutineScope) {
        try {
            producer = KafkaProducer(KafkaConfig.producerProperties())
            logger.info("Kafka FuelPriceProducer started — publishing to ${KafkaConfig.TOPIC_FUEL_PRICES}")

            job = scope.launch {
                while (isActive) {
                    delay(30_000) // Publish price update every 30 seconds
                    publishPriceUpdate()
                }
            }
        } catch (e: Exception) {
            logger.warn("Kafka not available — FuelPriceProducer running in offline mode: ${e.message}")
            // Run in offline mode: still update prices in memory for the API
            job = scope.launch {
                while (isActive) {
                    delay(30_000)
                    simulatePriceChange()
                }
            }
        }
    }

    fun stop() {
        job?.cancel()
        producer?.close()
        logger.info("FuelPriceProducer stopped")
    }

    fun getCurrentPrices(): Map<String, Double> = currentPrices.toMap()

    private fun publishPriceUpdate() {
        val fuelType = SimulationService.FUEL_PROFILES.keys.random()
        val previousPrice = currentPrices[fuelType] ?: return
        val newPrice = simulatePriceFluctuation(fuelType, previousPrice)

        currentPrices[fuelType] = newPrice

        val changePercent = ((newPrice - previousPrice) / previousPrice) * 100

        val event = FuelPriceEvent(
            fuelType = fuelType,
            price = Math.round(newPrice * 1000.0) / 1000.0,
            previousPrice = previousPrice,
            changePercent = Math.round(changePercent * 100.0) / 100.0,
            timestamp = System.currentTimeMillis()
        )

        try {
            val record = ProducerRecord(
                KafkaConfig.TOPIC_FUEL_PRICES,
                fuelType,
                json.encodeToString(event)
            )
            producer?.send(record) { metadata, exception ->
                if (exception != null) {
                    logger.error("Failed to publish price event: ${exception.message}")
                } else {
                    logger.debug("Price event published: $fuelType → ${event.price}€ (${if (changePercent >= 0) "+" else ""}${event.changePercent}%) [offset=${metadata.offset()}]")
                }
            }
        } catch (e: Exception) {
            logger.error("Kafka send error: ${e.message}")
        }
    }

    private fun simulatePriceChange() {
        val fuelType = SimulationService.FUEL_PROFILES.keys.random()
        val previousPrice = currentPrices[fuelType] ?: return
        currentPrices[fuelType] = simulatePriceFluctuation(fuelType, previousPrice)
    }

    /**
     * Simulate realistic fuel price fluctuations.
     * - Daily variation: ±0.5% to ±2% (rare spikes up to ±5%)
     * - Mean-reverting toward default price
     * - Geopolitical shock simulation (rare large jumps)
     */
    private fun simulatePriceFluctuation(fuelType: String, currentPrice: Double): Double {
        val defaultPrice = SimulationService.FUEL_PROFILES[fuelType]?.defaultPrice ?: currentPrice

        // Base random walk: ±0.5% to ±2%
        val baseChange = Random.nextDouble(-0.02, 0.02)

        // Mean reversion toward default (pulls back ~10% of the gap)
        val meanReversion = (defaultPrice - currentPrice) / defaultPrice * 0.1

        // Rare geopolitical shock (1% chance of ±3-5% spike)
        val shock = if (Random.nextDouble() < 0.01) {
            Random.nextDouble(-0.05, 0.05)
        } else 0.0

        val totalChange = baseChange + meanReversion + shock
        val newPrice = currentPrice * (1 + totalChange)

        // Clamp to reasonable bounds (±30% of default)
        val minPrice = defaultPrice * 0.7
        val maxPrice = defaultPrice * 1.3

        return newPrice.coerceIn(minPrice, maxPrice)
    }
}
