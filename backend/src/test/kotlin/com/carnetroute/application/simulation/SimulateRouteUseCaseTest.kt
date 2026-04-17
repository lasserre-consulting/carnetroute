package com.carnetroute.application.simulation

import com.carnetroute.domain.history.HistoryRepository
import com.carnetroute.domain.shared.DomainException
import com.carnetroute.domain.simulation.SimulationEngine
import com.carnetroute.domain.simulation.SimulationRepository
import com.carnetroute.domain.simulation.vo.Coordinates
import com.carnetroute.domain.simulation.vo.CostBreakdown
import com.carnetroute.domain.simulation.vo.Route
import com.carnetroute.domain.vehicle.vo.FuelType
import com.carnetroute.infrastructure.routing.RoutingPort
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk

/**
 * Tests unitaires pour SimulateRouteUseCase.
 *
 * ATTENTION — bugs connus dans SimulateRouteUseCase à la date de création des tests :
 *  1. buildCostBreakdown() est appelé avec (route = Route, fuelType, ...) alors que
 *     SimulationEngine attend (distanceKm, selectedFuelType, trafficFactor, baseDurationMinutes).
 *  2. estimateTrafficFactor() est appelé mais n'existe pas dans SimulationEngine.
 *  3. Simulation.create() est appelé avec (fromCoordinates, fromLabel, toCoordinates, toLabel,
 *     trafficMode, fuelType, costBreakdown) alors que la signature réelle attend
 *     (userId, vehicleId, route, traffic, costs).
 *
 * En conséquence, SimulationEngine est mocké ici pour isoler le use case et permettre
 * l'écriture des tests dès maintenant. Ils seront green une fois le use case corrigé.
 */
class SimulateRouteUseCaseTest : BehaviorSpec({

    val routingPort = mockk<RoutingPort>()
    val simulationEngine = mockk<SimulationEngine>()
    val simulationRepository = mockk<SimulationRepository>(relaxed = true)
    val historyRepository = mockk<HistoryRepository>(relaxed = true)

    val useCase = SimulateRouteUseCase(
        routingPort = routingPort,
        simulationEngine = simulationEngine,
        simulationRepository = simulationRepository,
        historyRepository = historyRepository
    )

    // ─────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────
    val paris = Coordinates(48.8566, 2.3522, "Paris")
    val lyon = Coordinates(45.7640, 4.8357, "Lyon")
    val toulouse = Coordinates(43.6047, 1.4442, "Toulouse")

    fun routePariLyon() = Route(from = paris, to = lyon, distanceKm = 470.0, durationMinutes = 270.0)
    fun routeParisToulouse() = Route(from = paris, to = toulouse, distanceKm = 680.0, durationMinutes = 360.0)

    fun stubBreakdown(fuelType: FuelType = FuelType.SP95) = CostBreakdown(
        fuelType = fuelType.name,
        pricePerUnit = fuelType.defaultPricePerUnit,
        consumptionPer100km = fuelType.defaultConsumptionPer100km,
        fuelConsumedTotal = 30.0,
        costTotal = 55.0,
        durationAdjustedMinutes = 270.0,
        comparison = FuelType.values().associate { ft ->
            ft.name to com.carnetroute.domain.simulation.vo.ComparisonEntry(
                fuelType = ft.name,
                pricePerUnit = ft.defaultPricePerUnit,
                consumptionPer100km = ft.defaultConsumptionPer100km,
                fuelConsumed = 30.0,
                totalCost = 50.0,
                unit = ft.unit
            )
        }
    )

    // ─────────────────────────────────────────────
    // Scénario nominal : Paris → Lyon en SP95
    // ─────────────────────────────────────────────
    given("une requête valide Paris → Lyon") {

        coEvery { routingPort.getRoute(any(), any()) } returns routePariLyon()
        every { simulationEngine.buildCostBreakdown(any(), any(), any(), any(), any(), any()) } returns stubBreakdown(FuelType.SP95)
        every { simulationEngine.estimateTrafficFactor(any(), any()) } returns 1.0
        every { simulationEngine.calculateTrafficFactor(any(), any()) } returns 1.0

        `when`("on simule avec SP95 en mode manuel") {
            val result = useCase.execute(
                SimulateRouteRequest(
                    fromLat = 48.8566, fromLng = 2.3522, fromLabel = "Paris",
                    toLat = 45.7640, toLng = 4.8357, toLabel = "Lyon",
                    fuelType = "SP95"
                )
            )

            then("le résultat ne doit pas être null") {
                result shouldNotBe null
            }

            then("le coût total doit être strictement positif") {
                (result.costs.costTotal > 0.0) shouldBe true
            }

            then("la comparaison doit contenir les 6 types de carburant") {
                result.costs.comparison.size shouldBe 6
            }

            then("le type de carburant du résultat est SP95") {
                result.costs.fuelType shouldBe "SP95"
            }
        }

        `when`("on simule avec ELECTRIC") {
            every { simulationEngine.buildCostBreakdown(any(), any(), any(), any(), any(), any()) } returns stubBreakdown(FuelType.ELECTRIC)

            val result = useCase.execute(
                SimulateRouteRequest(
                    fromLat = 48.8566, fromLng = 2.3522, fromLabel = "Paris",
                    toLat = 45.7640, toLng = 4.8357, toLabel = "Lyon",
                    fuelType = "ELECTRIC"
                )
            )

            then("le type de carburant du résultat est ELECTRIC") {
                result.costs.fuelType shouldBe "ELECTRIC"
            }
        }
    }

    // ─────────────────────────────────────────────
    // Scénario saveToHistory
    // ─────────────────────────────────────────────
    given("une requête avec saveToHistory=true et un userId") {

        coEvery { routingPort.getRoute(any(), any()) } returns routePariLyon()
        every { simulationEngine.buildCostBreakdown(any(), any(), any(), any(), any(), any()) } returns stubBreakdown()
        every { simulationEngine.estimateTrafficFactor(any(), any()) } returns 1.0
        every { simulationEngine.calculateTrafficFactor(any(), any()) } returns 1.0
        coEvery { simulationRepository.save(any()) } answers { firstArg() }

        `when`("on exécute la simulation") {
            useCase.execute(
                SimulateRouteRequest(
                    fromLat = 48.8566, fromLng = 2.3522,
                    toLat = 45.7640, toLng = 4.8357,
                    fuelType = "SP95",
                    userId = "user-123",
                    saveToHistory = true
                )
            )

            then("la simulation doit être sauvegardée via simulationRepository") {
                coVerify(exactly = 1) { simulationRepository.save(any()) }
            }

            then("l'historique doit être sauvegardé via historyRepository") {
                coVerify(exactly = 1) { historyRepository.save(any()) }
            }
        }
    }

    // ─────────────────────────────────────────────
    // Sans userId → pas de sauvegarde en base
    // ─────────────────────────────────────────────
    given("une requête sans userId") {

        coEvery { routingPort.getRoute(any(), any()) } returns routePariLyon()
        every { simulationEngine.buildCostBreakdown(any(), any(), any(), any(), any(), any()) } returns stubBreakdown()
        every { simulationEngine.estimateTrafficFactor(any(), any()) } returns 1.0
        every { simulationEngine.calculateTrafficFactor(any(), any()) } returns 1.0

        `when`("on exécute la simulation anonyme") {
            useCase.execute(
                SimulateRouteRequest(
                    fromLat = 48.8566, fromLng = 2.3522,
                    toLat = 45.7640, toLng = 4.8357,
                    fuelType = "SP95"
                    // userId = null (défaut)
                )
            )

            then("le repository de simulation ne doit PAS être appelé") {
                coVerify(exactly = 0) { simulationRepository.save(any()) }
            }

            then("l'historique ne doit PAS être sauvegardé") {
                coVerify(exactly = 0) { historyRepository.save(any()) }
            }
        }
    }

    // ─────────────────────────────────────────────
    // Scénario fuelType invalide
    // ─────────────────────────────────────────────
    given("un fuelType invalide") {

        `when`("on simule avec la valeur 'INCONNU'") {
            then("doit lever DomainException.InvalidFuelType") {
                shouldThrow<DomainException.InvalidFuelType> {
                    useCase.execute(
                        SimulateRouteRequest(
                            fromLat = 48.8566, fromLng = 2.3522,
                            toLat = 45.7640, toLng = 4.8357,
                            fuelType = "INCONNU"
                        )
                    )
                }
            }
        }

        `when`("on simule avec une chaîne vide") {
            then("doit lever DomainException.InvalidFuelType") {
                shouldThrow<DomainException.InvalidFuelType> {
                    useCase.execute(
                        SimulateRouteRequest(
                            fromLat = 48.8566, fromLng = 2.3522,
                            toLat = 45.7640, toLng = 4.8357,
                            fuelType = ""
                        )
                    )
                }
            }
        }
    }
})
