package com.carnetroute.infrastructure.messaging

import com.carnetroute.domain.fuelprice.FuelPrice
import io.nats.client.Connection
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class FuelPriceProducer(private val nats: Connection) {

    companion object {
        const val SUBJECT = "carnetroute.fuel.prices"
    }

    fun publish(prices: List<FuelPrice>) {
        val json = Json.encodeToString(prices)
        nats.publish(SUBJECT, json.toByteArray())
    }
}
