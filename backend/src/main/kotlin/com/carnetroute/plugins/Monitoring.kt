package com.carnetroute.plugins

import io.ktor.server.application.*
import io.ktor.server.metrics.micrometer.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import org.koin.ktor.ext.inject

fun Application.configureMonitoring() {
    val meterRegistry: MeterRegistry by inject()

    install(MicrometerMetrics) {
        registry = meterRegistry
    }

    routing {
        // Endpoint Prometheus — non authentifié, accessible par le scraper de métriques
        get("/metrics") {
            when (val registry = meterRegistry) {
                is PrometheusMeterRegistry -> call.respondText(registry.scrape())
                else -> call.respondText("# Metrics registry is not Prometheus\n")
            }
        }
    }
}
