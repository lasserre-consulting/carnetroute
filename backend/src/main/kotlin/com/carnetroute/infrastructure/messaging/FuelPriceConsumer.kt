package com.carnetroute.infrastructure.messaging

import com.carnetroute.domain.fuelprice.FuelPrice
import com.carnetroute.domain.fuelprice.FuelPriceRepository
import io.nats.client.Connection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

class FuelPriceConsumer(
    private val nats: Connection,
    private val fuelPriceRepository: FuelPriceRepository
) {
    private val logger = LoggerFactory.getLogger(FuelPriceConsumer::class.java)
    private var job: Job? = null

    fun start(scope: CoroutineScope) {
        val dispatcher = nats.createDispatcher { msg ->
            scope.launch {
                try {
                    val prices = Json.decodeFromString<List<FuelPrice>>(String(msg.data))
                    fuelPriceRepository.saveAll(prices)
                    logger.info("Updated ${prices.size} fuel prices from NATS")
                } catch (e: Exception) {
                    logger.error("Error processing fuel price update", e)
                }
            }
        }
        dispatcher.subscribe(FuelPriceProducer.SUBJECT)
        logger.info("FuelPriceConsumer started, listening on ${FuelPriceProducer.SUBJECT}")
    }

    fun stop() {
        job?.cancel()
    }
}
