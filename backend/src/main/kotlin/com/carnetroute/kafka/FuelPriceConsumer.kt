package com.carnetroute.kafka

import com.carnetroute.models.FuelPriceAlert
import com.carnetroute.models.FuelPriceEvent
import com.carnetroute.services.SimulationService
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
 * Consumes fuel price events from [carnetroute.fuel.prices],
 * evaluates alert thresholds, publishes alerts to [carnetroute.fuel.alerts],
 * and notifies connected WebSocket clients in real time.
 */
class FuelPriceConsumer {

    private val logger = LoggerFactory.getLogger(FuelPriceConsumer::class.java)
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private var consumer: KafkaConsumer<String, String>? = null
    private var alertProducer: KafkaProducer<String, String>? = null
    private var job: Job? = null

    // Alert thresholds (percentage change that triggers an alert)
    private val thresholds = mapOf(
        "info" to 1.0,       // ±1% → info
        "warning" to 3.0,    // ±3% → warning
        "critical" to 5.0    // ±5% → critical
    )

    // Recent alerts kept in memory for the REST API
    private val recentAlerts = CopyOnWriteArrayList<FuelPriceAlert>()
    private val maxAlerts = 50

    // WebSocket broadcast callback (set by Application.kt)
    var onAlert: ((FuelPriceAlert) -> Unit)? = null

    fun start(scope: CoroutineScope) {
        try {
            consumer = KafkaConsumer<String, String>(KafkaConfig.consumerProperties()).apply {
                subscribe(listOf(KafkaConfig.TOPIC_FUEL_PRICES))
            }
            alertProducer = KafkaProducer(KafkaConfig.producerProperties())
            logger.info("Kafka FuelPriceConsumer started — listening on ${KafkaConfig.TOPIC_FUEL_PRICES}")

            job = scope.launch(Dispatchers.IO) {
                while (isActive) {
                    try {
                        val records = consumer?.poll(Duration.ofMillis(500)) ?: continue
                        for (record in records) {
                            processEvent(record.value())
                        }
                    } catch (e: Exception) {
                        if (isActive) {
                            logger.error("Consumer poll error: ${e.message}")
                            delay(5000)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            logger.warn("Kafka not available — FuelPriceConsumer running in offline mode: ${e.message}")
        }
    }

    fun stop() {
        job?.cancel()
        consumer?.close()
        alertProducer?.close()
        logger.info("FuelPriceConsumer stopped")
    }

    fun getRecentAlerts(): List<FuelPriceAlert> = recentAlerts.toList()

    private fun processEvent(eventJson: String) {
        try {
            val event = json.decodeFromString<FuelPriceEvent>(eventJson)
            val absChange = Math.abs(event.changePercent)

            // Determine severity
            val severity = when {
                absChange >= thresholds["critical"]!! -> "critical"
                absChange >= thresholds["warning"]!! -> "warning"
                absChange >= thresholds["info"]!! -> "info"
                else -> return // Below threshold, no alert
            }

            val profile = SimulationService.FUEL_PROFILES[event.fuelType] ?: return
            val direction = if (event.changePercent >= 0) "up" else "down"
            val directionEmoji = if (direction == "up") "📈" else "📉"
            val sign = if (direction == "up") "+" else ""

            val alert = FuelPriceAlert(
                id = UUID.randomUUID().toString(),
                fuelType = event.fuelType,
                fuelLabel = profile.label,
                fuelIcon = profile.icon,
                currentPrice = event.price,
                previousPrice = event.previousPrice,
                changePercent = event.changePercent,
                direction = direction,
                severity = severity,
                message = "$directionEmoji ${profile.label} : ${event.price}€/${profile.priceUnit.removePrefix("€/")} (${sign}${event.changePercent}%)",
                timestamp = event.timestamp
            )

            // Store in memory
            recentAlerts.add(0, alert)
            while (recentAlerts.size > maxAlerts) {
                recentAlerts.removeAt(recentAlerts.size - 1)
            }

            // Publish alert to Kafka
            try {
                alertProducer?.send(
                    ProducerRecord(
                        KafkaConfig.TOPIC_FUEL_ALERTS,
                        event.fuelType,
                        json.encodeToString(alert)
                    )
                )
            } catch (e: Exception) {
                logger.error("Failed to publish alert: ${e.message}")
            }

            // Broadcast to WebSocket clients
            onAlert?.invoke(alert)

            logger.info("ALERT [$severity] ${alert.message}")

        } catch (e: Exception) {
            logger.error("Failed to process price event: ${e.message}")
        }
    }
}
