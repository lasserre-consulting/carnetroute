package com.carnetroute.infrastructure.kafka

import com.carnetroute.domain.model.FuelPriceAlert
import com.carnetroute.domain.service.SimulationEngine
import kotlinx.coroutines.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Consommateur Kafka — lit les événements de prix sur [carnetroute.fuel.prices],
 * génère des alertes selon les seuils, publie sur [carnetroute.fuel.alerts]
 * et diffuse aux clients WebSocket connectés.
 */
class FuelPriceConsumer {

    private val logger = LoggerFactory.getLogger(FuelPriceConsumer::class.java)
    private val json   = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private var consumer:      KafkaConsumer<String, String>? = null
    private var alertProducer: KafkaProducer<String, String>? = null
    private var job:           Job? = null

    private val thresholds = mapOf("info" to 1.0, "warning" to 3.0, "critical" to 5.0)

    private val recentAlerts = CopyOnWriteArrayList<FuelPriceAlert>()
    private val maxAlerts    = 50

    /** Callback de diffusion WebSocket — défini par Application.kt. */
    var onAlert: ((FuelPriceAlert) -> Unit)? = null

    fun start(scope: CoroutineScope) {
        try {
            consumer = KafkaConsumer<String, String>(KafkaConfig.consumerProperties()).apply {
                subscribe(listOf(KafkaConfig.TOPIC_FUEL_PRICES))
            }
            alertProducer = KafkaProducer(KafkaConfig.producerProperties())
            logger.info("FuelPriceConsumer démarré → ${KafkaConfig.TOPIC_FUEL_PRICES}")

            job = scope.launch(Dispatchers.IO) {
                while (isActive) {
                    try {
                        val records = consumer?.poll(Duration.ofMillis(500)) ?: continue
                        for (record in records) processEvent(record.value())
                    } catch (e: Exception) {
                        if (isActive) { logger.error("Erreur poll : ${e.message}"); delay(5000) }
                    }
                }
            }
        } catch (e: Exception) {
            logger.warn("Kafka indisponible — mode hors-ligne : ${e.message}")
        }
    }

    fun stop() {
        job?.cancel()
        consumer?.close()
        alertProducer?.close()
        logger.info("FuelPriceConsumer arrêté")
    }

    fun getRecentAlerts(): List<FuelPriceAlert> = recentAlerts.toList()

    private fun processEvent(eventJson: String) {
        try {
            val event     = json.decodeFromString<FuelPriceEvent>(eventJson)
            val absChange = Math.abs(event.changePercent)

            val severity = when {
                absChange >= thresholds["critical"]!! -> "critical"
                absChange >= thresholds["warning"]!!  -> "warning"
                absChange >= thresholds["info"]!!     -> "info"
                else -> return
            }

            val profile   = SimulationEngine.FUEL_PROFILES[event.fuelType] ?: return
            val direction = if (event.changePercent >= 0) "up" else "down"
            val emoji     = if (direction == "up") "📈" else "📉"
            val sign      = if (direction == "up") "+" else ""

            val alert = FuelPriceAlert(
                id            = UUID.randomUUID().toString(),
                fuelType      = event.fuelType,
                fuelLabel     = profile.label,
                fuelIcon      = profile.icon,
                currentPrice  = event.price,
                previousPrice = event.previousPrice,
                changePercent = event.changePercent,
                direction     = direction,
                severity      = severity,
                message       = "$emoji ${profile.label} : ${event.price}€/${profile.priceUnit.removePrefix("€/")} (${sign}${event.changePercent}%)",
                timestamp     = event.timestamp
            )

            recentAlerts.add(0, alert)
            while (recentAlerts.size > maxAlerts) recentAlerts.removeAt(recentAlerts.size - 1)

            try {
                alertProducer?.send(ProducerRecord(KafkaConfig.TOPIC_FUEL_ALERTS, event.fuelType, json.encodeToString(alert)))
            } catch (e: Exception) {
                logger.error("Échec publication alerte : ${e.message}")
            }

            onAlert?.invoke(alert)
            logger.info("ALERTE [$severity] ${alert.message}")

        } catch (e: Exception) {
            logger.error("Erreur traitement événement prix : ${e.message}")
        }
    }
}
