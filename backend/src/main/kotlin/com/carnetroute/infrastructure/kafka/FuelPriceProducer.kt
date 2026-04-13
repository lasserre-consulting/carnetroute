package com.carnetroute.infrastructure.kafka

import com.carnetroute.domain.service.SimulationEngine
import kotlinx.coroutines.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory
import kotlin.random.Random

/**
 * Producteur Kafka — simule des fluctuations de prix carburant sur [carnetroute.fuel.prices].
 *
 * En production, ce composant se connecterait à un flux de données réel
 * (ex : API prix-carburants du gouvernement français).
 * Ici, il simule des variations réalistes à des fins de démonstration.
 */
class FuelPriceProducer {

    private val logger = LoggerFactory.getLogger(FuelPriceProducer::class.java)
    private val json   = Json { encodeDefaults = true }
    private var producer: KafkaProducer<String, String>? = null
    private var job: Job? = null

    private val currentPrices = mutableMapOf<String, Double>().apply {
        SimulationEngine.FUEL_PROFILES.forEach { (key, profile) -> put(key, profile.defaultPrice) }
    }

    fun start(scope: CoroutineScope) {
        try {
            producer = KafkaProducer(KafkaConfig.producerProperties())
            logger.info("FuelPriceProducer démarré → topic ${KafkaConfig.TOPIC_FUEL_PRICES}")
            job = scope.launch {
                while (isActive) {
                    delay(30_000)
                    publishPriceUpdate()
                }
            }
        } catch (e: Exception) {
            logger.warn("Kafka indisponible — mode hors-ligne : ${e.message}")
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
        logger.info("FuelPriceProducer arrêté")
    }

    fun getCurrentPrices(): Map<String, Double> = currentPrices.toMap()

    private fun publishPriceUpdate() {
        val fuelType      = SimulationEngine.FUEL_PROFILES.keys.random()
        val previousPrice = currentPrices[fuelType] ?: return
        val newPrice      = simulatePriceFluctuation(fuelType, previousPrice)
        currentPrices[fuelType] = newPrice

        val changePercent = ((newPrice - previousPrice) / previousPrice) * 100
        val event = FuelPriceEvent(
            fuelType      = fuelType,
            price         = Math.round(newPrice * 1000.0) / 1000.0,
            previousPrice = previousPrice,
            changePercent = Math.round(changePercent * 100.0) / 100.0,
            timestamp     = System.currentTimeMillis()
        )

        try {
            val record = ProducerRecord(KafkaConfig.TOPIC_FUEL_PRICES, fuelType, json.encodeToString(event))
            producer?.send(record) { metadata, exception ->
                if (exception != null) logger.error("Échec publication prix : ${exception.message}")
                else logger.debug("Prix publié : $fuelType → ${event.price}€ (${event.changePercent}%) [offset=${metadata.offset()}]")
            }
        } catch (e: Exception) {
            logger.error("Erreur envoi Kafka : ${e.message}")
        }
    }

    private fun simulatePriceChange() {
        val fuelType      = SimulationEngine.FUEL_PROFILES.keys.random()
        val previousPrice = currentPrices[fuelType] ?: return
        currentPrices[fuelType] = simulatePriceFluctuation(fuelType, previousPrice)
    }

    /**
     * Fluctuation réaliste : marche aléatoire ± 0,5-2 %, réversion vers le prix par défaut,
     * choc géopolitique rare (1 % de chance de ±3-5 %).
     */
    private fun simulatePriceFluctuation(fuelType: String, currentPrice: Double): Double {
        val defaultPrice   = SimulationEngine.FUEL_PROFILES[fuelType]?.defaultPrice ?: currentPrice
        val baseChange     = Random.nextDouble(-0.02, 0.02)
        val meanReversion  = (defaultPrice - currentPrice) / defaultPrice * 0.1
        val shock          = if (Random.nextDouble() < 0.01) Random.nextDouble(-0.05, 0.05) else 0.0
        val newPrice       = currentPrice * (1 + baseChange + meanReversion + shock)
        return newPrice.coerceIn(defaultPrice * 0.7, defaultPrice * 1.3)
    }
}
