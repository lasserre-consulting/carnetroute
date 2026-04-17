package com.carnetroute.infrastructure.messaging

import io.nats.client.Connection
import io.nats.client.Nats
import io.nats.client.Options

object NATSFactory {
    fun connect(natsUrl: String): Connection {
        val options = Options.Builder()
            .server(natsUrl)
            .reconnectWait(java.time.Duration.ofSeconds(2))
            .maxReconnects(10)
            .connectionName("carnetroute-backend")
            .build()
        return Nats.connect(options)
    }
}
