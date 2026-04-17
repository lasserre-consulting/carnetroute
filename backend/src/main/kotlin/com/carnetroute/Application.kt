package com.carnetroute

import com.carnetroute.plugins.configureHTTP
import com.carnetroute.plugins.configureMonitoring
import com.carnetroute.plugins.configureRouting
import com.carnetroute.plugins.configureSecurity
import com.carnetroute.plugins.configureSerialization
import com.carnetroute.plugins.appModule
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.websocket.*
import org.koin.ktor.plugin.Koin
import kotlin.time.Duration.Companion.seconds

fun main(args: Array<String>) = EngineMain.main(args)

fun Application.module() {
    // 1. Koin dependency injection
    install(Koin) {
        modules(appModule(environment.config))
    }

    // 2. Serialization
    configureSerialization()

    // 3. HTTP (CORS, DefaultHeaders, StatusPages, CallLogging)
    configureHTTP()

    // 4. Security (JWT authentication)
    configureSecurity()

    // 5. Monitoring (Micrometer / Prometheus)
    configureMonitoring()

    // 6. WebSockets
    install(WebSockets) {
        pingPeriod = 15.seconds
        timeout = 15.seconds
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }

    // 7. Routing
    configureRouting()
}
