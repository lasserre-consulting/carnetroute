package com.carnetroute.application.usecase

import com.carnetroute.domain.model.Coordinates
import com.carnetroute.domain.model.RouteInfo
import com.carnetroute.domain.model.SimulationRequest
import com.carnetroute.domain.port.GeocodingPort
import com.carnetroute.domain.port.RoutingPort
import com.carnetroute.domain.service.SimulationEngine
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.mockk.*

class SimulationUseCasesTest : DescribeSpec({

    val routingPort   = mockk<RoutingPort>()
    val geocodingPort = mockk<GeocodingPort>()
    val engine        = SimulationEngine()

    val paris    = Coordinates(48.8566, 2.3522, "Paris")
    val toulouse = Coordinates(43.6047, 1.4442, "Toulouse")
    val mockRoute = RouteInfo(distanceKm = 711.0, durationMin = 390.0, source = "osrm")

    afterTest { clearMocks(routingPort, geocodingPort) }

    describe("SimulateRouteUseCase") {
        it("délègue le routage au port et calcule la simulation") {
            coEvery { routingPort.getRoute(paris, toulouse, false) } returns mockRoute

            val request = SimulationRequest(from = paris, to = toulouse, fuelType = "sp95")
            val result  = SimulateRouteUseCase(routingPort, engine).execute(request)

            result.distanceKm    shouldBe 711.0
            result.routingSource shouldBe "osrm"
            result.comparison.size shouldBe 6
            coVerify(exactly = 1) { routingPort.getRoute(paris, toulouse, false) }
        }

        it("transmet avoidTolls au port de routage") {
            coEvery { routingPort.getRoute(paris, toulouse, true) } returns mockRoute

            val request = SimulationRequest(from = paris, to = toulouse, fuelType = "diesel", avoidTolls = true)
            SimulateRouteUseCase(routingPort, engine).execute(request)

            coVerify(exactly = 1) { routingPort.getRoute(paris, toulouse, true) }
        }

        it("propage IllegalArgumentException pour un carburant inconnu") {
            coEvery { routingPort.getRoute(any(), any(), any()) } returns mockRoute

            val request = SimulationRequest(from = paris, to = toulouse, fuelType = "charbon")
            shouldThrow<IllegalArgumentException> {
                SimulateRouteUseCase(routingPort, engine).execute(request)
            }
        }
    }

    describe("GenerateHeatmapUseCase") {
        it("délègue le routage et génère une grille 7×24") {
            coEvery { routingPort.getRoute(paris, toulouse, false) } returns mockRoute

            val request = SimulationRequest(from = paris, to = toulouse, fuelType = "e85")
            val result  = GenerateHeatmapUseCase(routingPort, engine).execute(request)

            result.grid.size shouldBe 7
            result.grid.forEach { day -> day.size shouldBe 24 }
            result.distanceKm shouldBe 711.0
            coVerify(exactly = 1) { routingPort.getRoute(paris, toulouse, false) }
        }

        it("utilise le prix personnalisé pour les calculs") {
            coEvery { routingPort.getRoute(any(), any(), any()) } returns mockRoute

            val base   = SimulationRequest(from = paris, to = toulouse, fuelType = "sp95")
            val custom = base.copy(customPrice = 3.0)

            val heatmapBase   = GenerateHeatmapUseCase(routingPort, engine).execute(base)
            val heatmapCustom = GenerateHeatmapUseCase(routingPort, engine).execute(custom)

            // Lundi 8h : coût plus élevé avec prix personnalisé (3.0 > 1.85 par défaut)
            heatmapCustom.grid[0][8].cost shouldBeGreaterThan heatmapBase.grid[0][8].cost
        }
    }

    describe("GetFuelProfilesUseCase") {
        it("retourne les 6 profils carburant") {
            val profiles = GetFuelProfilesUseCase(engine).execute()
            profiles.size shouldBe 6
        }
    }

    describe("GeocodeUseCase") {
        it("délègue au port de géocodage") {
            val expectedJson = """{"type":"FeatureCollection","features":[]}"""
            coEvery { geocodingPort.autocomplete("Toulouse", 7) } returns expectedJson

            val result = GeocodeUseCase(geocodingPort).execute("Toulouse", 7)

            result shouldBe expectedJson
            coVerify(exactly = 1) { geocodingPort.autocomplete("Toulouse", 7) }
        }

        it("utilise la limite par défaut (7)") {
            coEvery { geocodingPort.autocomplete("Paris", 7) } returns """{"features":[]}"""

            GeocodeUseCase(geocodingPort).execute("Paris")

            coVerify(exactly = 1) { geocodingPort.autocomplete("Paris", 7) }
        }
    }

    describe("ReverseGeocodeUseCase") {
        it("délègue le géocodage inverse au port") {
            val expectedJson = """{"type":"FeatureCollection","features":[]}"""
            coEvery { geocodingPort.reverse(43.6047, 1.4442) } returns expectedJson

            val result = ReverseGeocodeUseCase(geocodingPort).execute(43.6047, 1.4442)

            result shouldBe expectedJson
            coVerify(exactly = 1) { geocodingPort.reverse(43.6047, 1.4442) }
        }
    }
})
