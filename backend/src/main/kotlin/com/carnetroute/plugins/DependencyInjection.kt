package com.carnetroute.plugins

import com.carnetroute.application.fuelprice.GetFuelPricesUseCase
import com.carnetroute.application.geocoding.AutocompleteUseCase
import com.carnetroute.application.history.GetJourneyHistoryUseCase
import com.carnetroute.application.history.GetUserStatisticsUseCase
import com.carnetroute.application.simulation.GenerateHeatmapUseCase
import com.carnetroute.application.simulation.GetSimulationHistoryUseCase
import com.carnetroute.application.simulation.SimulateRouteUseCase
import com.carnetroute.application.user.AuthenticateUserUseCase
import com.carnetroute.application.user.CreateUserUseCase
import com.carnetroute.application.user.GetUserProfileUseCase
import com.carnetroute.application.user.UpdateUserProfileUseCase
import com.carnetroute.application.vehicle.CreateVehicleUseCase
import com.carnetroute.application.vehicle.DeleteVehicleUseCase
import com.carnetroute.application.vehicle.GetVehiclesUseCase
import com.carnetroute.application.vehicle.UpdateVehicleUseCase
import com.carnetroute.domain.fuelprice.FuelPricePort
import com.carnetroute.domain.fuelprice.FuelPriceRepository
import com.carnetroute.domain.history.HistoryRepository
import com.carnetroute.domain.simulation.SimulationEngine
import com.carnetroute.domain.simulation.SimulationRepository
import com.carnetroute.domain.user.UserRepository
import com.carnetroute.domain.vehicle.VehicleRepository
import com.carnetroute.infrastructure.cache.RedisCacheService
import com.carnetroute.infrastructure.persistence.repositories.FuelPriceRepositoryImpl
import com.carnetroute.infrastructure.fuelprice.StaticFuelPriceAdapter
import com.carnetroute.infrastructure.geocoding.AddressGovGeocodingAdapter
import com.carnetroute.infrastructure.geocoding.GeocodingPort
import com.carnetroute.infrastructure.messaging.NATSFactory
import com.carnetroute.infrastructure.monitoring.SimulationMetrics
import com.carnetroute.infrastructure.persistence.DatabaseFactory
import com.carnetroute.infrastructure.persistence.repositories.DomainHistoryRepositoryImpl
import com.carnetroute.infrastructure.persistence.repositories.DomainSimulationRepositoryImpl
import com.carnetroute.infrastructure.persistence.repositories.UserRepositoryImpl
import com.carnetroute.infrastructure.persistence.repositories.VehicleRepositoryImpl
import com.carnetroute.infrastructure.routing.OsrmRoutingAdapter
import com.carnetroute.infrastructure.routing.RoutingPort
import com.carnetroute.infrastructure.security.BCryptPasswordEncoder
import com.carnetroute.infrastructure.security.JwtService
import com.carnetroute.infrastructure.security.JwtServiceImpl
import com.carnetroute.infrastructure.security.PasswordEncoder
import io.ktor.server.config.*
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import org.koin.dsl.module

fun appModule(config: ApplicationConfig) = module {

    // ── Prometheus registry partagé ──────────────────────────────────────────
    single<MeterRegistry> {
        PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    }

    // ── Infrastructure : base de données ────────────────────────────────────
    single {
        DatabaseFactory(config).also { it.init() }
    }

    // ── Infrastructure : cache Redis ─────────────────────────────────────────
    single {
        val redisUrl = config.property("app.redis.url").getString()
        RedisCacheService(redisUrl)
    }

    // ── Infrastructure : messagerie NATS ────────────────────────────────────
    single {
        val natsUrl = config.property("app.nats.url").getString()
        NATSFactory.connect(natsUrl)
    }

    // ── Infrastructure : sécurité ────────────────────────────────────────────
    single<JwtService> {
        JwtServiceImpl(
            secret = config.property("app.jwt.secret").getString(),
            issuer = config.property("app.jwt.issuer").getString(),
            audience = config.property("app.jwt.audience").getString(),
            expiresInSeconds = config.propertyOrNull("app.jwt.expiresInSeconds")
                ?.getString()?.toLongOrNull() ?: 3600L,
            refreshExpiresInSeconds = config.propertyOrNull("app.jwt.refreshExpiresInSeconds")
                ?.getString()?.toLongOrNull() ?: 604800L,
        )
    }
    single<PasswordEncoder> { BCryptPasswordEncoder() }

    // ── Infrastructure : repositories ───────────────────────────────────────
    single<UserRepository> { UserRepositoryImpl(get()) }
    single<VehicleRepository> { VehicleRepositoryImpl(get()) }
    single<SimulationRepository> { DomainSimulationRepositoryImpl(get()) }
    single<HistoryRepository> { DomainHistoryRepositoryImpl(get()) }
    single<FuelPriceRepository> { FuelPriceRepositoryImpl(get()) }

    // ── Infrastructure : adaptateurs externes ────────────────────────────────
    single<RoutingPort> { OsrmRoutingAdapter() }
    single<GeocodingPort> { AddressGovGeocodingAdapter() }
    single<FuelPricePort> { StaticFuelPriceAdapter() }

    // ── Infrastructure : monitoring ──────────────────────────────────────────
    single { SimulationMetrics(get()) }

    // ── Domaine : moteur de simulation ───────────────────────────────────────
    single { SimulationEngine() }

    // ── Application : use cases utilisateurs ────────────────────────────────
    single { CreateUserUseCase(get(), get(), get()) }
    single { AuthenticateUserUseCase(get(), get(), get()) }
    single { GetUserProfileUseCase(get()) }
    single { UpdateUserProfileUseCase(get()) }

    // ── Application : use cases véhicules ───────────────────────────────────
    single { CreateVehicleUseCase(get()) }
    single { GetVehiclesUseCase(get()) }
    single { UpdateVehicleUseCase(get()) }
    single { DeleteVehicleUseCase(get()) }

    // ── Application : use cases simulation ──────────────────────────────────
    single { SimulateRouteUseCase(get(), get(), get(), get()) }
    single { GenerateHeatmapUseCase(get()) }
    single { GetSimulationHistoryUseCase(get()) }

    // ── Application : use cases carburants ──────────────────────────────────
    single { GetFuelPricesUseCase(get()) }

    // ── Application : use cases géocodage ───────────────────────────────────
    single { AutocompleteUseCase(get()) }

    // ── Application : use cases historique ──────────────────────────────────
    single { GetJourneyHistoryUseCase(get()) }
    single { GetUserStatisticsUseCase(get()) }
}
