package com.carnetroute.infrastructure.monitoring

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import java.time.Duration
import java.util.concurrent.atomic.AtomicLong

class SimulationMetrics(private val registry: MeterRegistry) {

    // Timer pour la durée des simulations, avec percentiles p50/p95/p99
    val simulationDuration: Timer = Timer.builder("simulation.duration")
        .description("Durée d'exécution d'une simulation")
        .publishPercentiles(0.5, 0.95, 0.99)
        .publishPercentileHistogram()
        .register(registry)

    // Gauge epoch seconds du dernier rafraîchissement des prix carburant
    private val lastFuelPriceUpdateEpoch = AtomicLong(0L)

    init {
        registry.gauge(
            "fuel_prices.last_update",
            lastFuelPriceUpdateEpoch
        ) { it.get().toDouble() }
    }

    // Compteur de simulations tagué par type de carburant
    fun simulationCounter(fuelType: String): Counter =
        Counter.builder("simulation.count")
            .description("Nombre de simulations effectuées")
            .tag("fuelType", fuelType)
            .register(registry)

    // Compteur d'erreurs API routing tagué par provider
    fun routingApiErrorCounter(provider: String): Counter =
        Counter.builder("routing.api.errors")
            .description("Nombre d'erreurs lors des appels API routing")
            .tag("provider", provider)
            .register(registry)

    // Timer de latence API routing tagué par provider
    fun routingApiLatencyTimer(provider: String): Timer =
        Timer.builder("routing.api.latency")
            .description("Latence des appels API routing")
            .publishPercentiles(0.5, 0.95, 0.99)
            .tag("provider", provider)
            .register(registry)

    // Met à jour le timestamp du dernier rafraîchissement des prix
    fun recordFuelPricesUpdate() {
        lastFuelPriceUpdateEpoch.set(System.currentTimeMillis() / 1000)
    }

    // Utilitaire pour chronométrer un bloc suspend et enregistrer dans un Timer
    fun <T> recordTimer(timer: Timer, block: () -> T): T {
        val sample = Timer.start(registry)
        return try {
            block()
        } finally {
            sample.stop(timer)
        }
    }
}
