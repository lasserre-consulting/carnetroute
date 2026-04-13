package com.carnetroute.domain.service

import com.carnetroute.domain.model.Coordinates
import com.carnetroute.domain.model.RouteInfo
import com.carnetroute.domain.model.SimulationRequest
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.doubles.shouldBeLessThan
import io.kotest.matchers.ints.shouldBeExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain

class SimulationEngineTest : DescribeSpec({

    val engine = SimulationEngine()

    describe("haversineKm") {
        it("Paris → Toulouse devrait être ~588 km") {
            val dist = engine.haversineKm(48.8566, 2.3522, 43.6047, 1.4442)
            dist shouldBeGreaterThan 580.0
            dist shouldBeLessThan 600.0
        }

        it("distance nulle pour le même point") {
            engine.haversineKm(48.8566, 2.3522, 48.8566, 2.3522) shouldBe 0.0
        }
    }

    describe("roadDistanceKm") {
        it("applique un facteur supérieur à 1.3 sur la distance haversine") {
            val paris    = Coordinates(48.8566, 2.3522)
            val toulouse = Coordinates(43.6047, 1.4442)
            val straight = engine.haversineKm(paris.lat, paris.lng, toulouse.lat, toulouse.lng)
            val road     = engine.roadDistanceKm(paris, toulouse)
            road shouldBeGreaterThan straight * 1.3
            road shouldBeLessThan    straight * 1.4
        }
    }

    describe("getTrafficFactor") {
        it("lundi 8h est heure de pointe (facteur ≥ 1.9)") {
            val factor = engine.getTrafficFactor(0, 8)
            factor shouldNotBe null
            factor shouldBeGreaterThan 1.8
        }

        it("dimanche 3h est minimal (facteur < 1.1)") {
            engine.getTrafficFactor(6, 3) shouldBeLessThan 1.1
        }

        it("vendredi 17h a un boost par rapport à mercredi 17h") {
            val friday    = engine.getTrafficFactor(4, 17)
            val wednesday = engine.getTrafficFactor(2, 17)
            friday shouldBeGreaterThan wednesday
        }

        it("dimanche 17h a un boost de retour par rapport à samedi 17h") {
            val sunday   = engine.getTrafficFactor(6, 17)
            val saturday = engine.getTrafficFactor(5, 17)
            sunday shouldBeGreaterThan saturday
        }
    }

    describe("getTrafficInfo") {
        it("facteur < 1.15 → Fluide") {
            val (label, icon) = engine.getTrafficInfo(1.1)
            label shouldBe "Fluide"
            icon  shouldBe "🟢"
        }

        it("facteur 2.0 → Embouteillage") {
            val (label, _) = engine.getTrafficInfo(2.0)
            label shouldBe "Embouteillage"
        }
    }

    describe("simulate") {
        val osrmRoute = RouteInfo(distanceKm = 711.0, durationMin = 390.0, source = "osrm")

        it("retourne un résultat valide pour sp95 en mode manuel fluide") {
            val request = SimulationRequest(
                from               = Coordinates(48.8566, 2.3522, "Paris"),
                to                 = Coordinates(43.6047, 1.4442, "Toulouse"),
                fuelType           = "sp95",
                trafficMode        = "manual",
                manualTrafficLevel = 0
            )
            val result = engine.simulate(request, osrmRoute)

            result.distanceKm      shouldBe 711.0
            result.routingSource   shouldBe "osrm"
            result.fuelUnit        shouldBe "L"
            result.totalCost       shouldBeGreaterThan 0.0
            result.comparison.size shouldBeExactly 6
        }

        it("l'électrique coûte moins cher que le diesel") {
            val request = SimulationRequest(
                from               = Coordinates(48.8566, 2.3522),
                to                 = Coordinates(43.6047, 1.4442),
                fuelType           = "diesel",
                trafficMode        = "manual",
                manualTrafficLevel = 0
            )
            val result     = engine.simulate(request, osrmRoute)
            val elecCost   = result.comparison.first { it.key == "electrique" }.cost
            val dieselCost = result.comparison.first { it.key == "diesel" }.cost
            elecCost shouldBeLessThan dieselCost
        }

        it("l'unité est kWh pour l'électrique") {
            val request = SimulationRequest(
                from     = Coordinates(48.8566, 2.3522),
                to       = Coordinates(43.6047, 1.4442),
                fuelType = "electrique"
            )
            engine.simulate(request, osrmRoute).fuelUnit shouldBe "kWh"
        }

        it("lève IllegalArgumentException pour un carburant inconnu") {
            val request = SimulationRequest(
                from     = Coordinates(48.8566, 2.3522),
                to       = Coordinates(43.6047, 1.4442),
                fuelType = "hydrogene"
            )
            val ex = shouldThrow<IllegalArgumentException> { engine.simulate(request, osrmRoute) }
            ex.message shouldContain "hydrogene"
        }

        it("le prix personnalisé augmente le coût total") {
            val base = SimulationRequest(
                from = Coordinates(48.8566, 2.3522), to = Coordinates(43.6047, 1.4442),
                fuelType = "sp95", trafficMode = "manual", manualTrafficLevel = 0
            )
            val custom = base.copy(customPrice = 3.0)
            val costBase   = engine.simulate(base, osrmRoute).totalCost
            val costCustom = engine.simulate(custom, osrmRoute).totalCost
            costCustom shouldBeGreaterThan costBase
        }

        it("le trafic automatique applique le facteur de l'heure de départ") {
            val request = SimulationRequest(
                from          = Coordinates(48.8566, 2.3522),
                to            = Coordinates(43.6047, 1.4442),
                fuelType      = "sp95",
                trafficMode   = "auto",
                departureDay  = 0,
                departureHour = 8
            )
            val result = engine.simulate(request, osrmRoute)
            result.trafficFactor shouldBeGreaterThan 1.8
        }
    }

    describe("generateHeatmapGrid") {
        val route = RouteInfo(distanceKm = 711.0, durationMin = 390.0, source = "osrm")

        it("génère une grille 7×24") {
            val heatmap = engine.generateHeatmapGrid(route, "sp95")
            heatmap.grid.size shouldBeExactly 7
            heatmap.grid.forEach { day -> day.size shouldBeExactly 24 }
        }

        it("propage la distance et le temps de base") {
            val heatmap = engine.generateHeatmapGrid(route, "diesel")
            heatmap.distanceKm  shouldBe 711.0
            heatmap.baseTimeMin shouldBe 390.0
        }

        it("lève IllegalArgumentException pour un carburant inconnu") {
            shouldThrow<IllegalArgumentException> {
                engine.generateHeatmapGrid(route, "charbon")
            }
        }

        it("l'heure de pointe du lundi a une durée plus longue que 3h du matin") {
            val heatmap   = engine.generateHeatmapGrid(route, "sp95")
            val peakHour  = heatmap.grid[0][8].durationMin
            val quietHour = heatmap.grid[0][3].durationMin
            peakHour shouldBeGreaterThan quietHour
        }
    }

    describe("FUEL_PROFILES") {
        it("contient les 6 types attendus") {
            SimulationEngine.FUEL_PROFILES.size shouldBeExactly 6
            SimulationEngine.FUEL_PROFILES.keys shouldBe setOf("sp95", "sp98", "diesel", "e85", "gpl", "electrique")
        }

        it("tous les profils ont des prix et consommations positifs") {
            SimulationEngine.FUEL_PROFILES.values.forEach { profile ->
                profile.defaultPrice shouldBeGreaterThan 0.0
                profile.consumption  shouldBeGreaterThan 0.0
            }
        }
    }
})
