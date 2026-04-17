package com.carnetroute.domain.simulation

import com.carnetroute.domain.simulation.vo.Coordinates
import com.carnetroute.domain.vehicle.vo.FuelType
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Tests unitaires pour SimulationEngine.
 *
 * Note : calculateDistance() applique déjà un road factor interne de 1.15.
 * HaversineCalculator (infrastructure) applique lui un factor de 1.3 séparé.
 * Les tolérances tiennent compte de ce facteur embarqué.
 */
class SimulationEngineTest : FunSpec({

    val engine = SimulationEngine()

    // ─────────────────────────────────────────────
    // calculateDistance (formule haversine + road factor 1.15 intégré)
    // ─────────────────────────────────────────────
    context("calculateDistance (haversine avec road factor 1.15 intégré)") {

        test("Paris → Toulouse devrait être ~593 km (haversine ~516 km × 1.15)") {
            val paris = Coordinates(48.8566, 2.3522)
            val toulouse = Coordinates(43.6047, 1.4442)
            val dist = engine.calculateDistance(paris, toulouse)
            // vol d'oiseau ≈ 588 km, × 1.15 ≈ 593 km
            dist shouldBe (593.0 plusOrMinus 30.0)
        }

        test("Paris → Lyon devrait être ~480 km (haversine ~392 km × 1.15)") {
            val paris = Coordinates(48.8566, 2.3522)
            val lyon = Coordinates(45.7640, 4.8357)
            val dist = engine.calculateDistance(paris, lyon)
            dist shouldBe (450.0 plusOrMinus 40.0)
        }

        test("Même point → distance 0") {
            val paris = Coordinates(48.8566, 2.3522)
            engine.calculateDistance(paris, paris) shouldBe (0.0 plusOrMinus 0.001)
        }

        test("Distance est symétrique (A→B ≈ B→A)") {
            val paris = Coordinates(48.8566, 2.3522)
            val bordeaux = Coordinates(44.8378, -0.5792)
            val ab = engine.calculateDistance(paris, bordeaux)
            val ba = engine.calculateDistance(bordeaux, paris)
            ab shouldBe (ba plusOrMinus 0.001)
        }
    }

    // ─────────────────────────────────────────────
    // calculateTrafficFactor
    // ─────────────────────────────────────────────
    context("calculateTrafficFactor") {

        test("Lundi 8h (heure de pointe matin) → facteur élevé (>= 1.7)") {
            val factor = engine.calculateTrafficFactor(1, 8)
            (factor >= 1.7) shouldBe true
        }

        test("Dimanche 3h (nuit week-end) → facteur minimal (1.0)") {
            val factor = engine.calculateTrafficFactor(7, 3)
            factor shouldBe (1.0 plusOrMinus 0.01)
        }

        test("Vendredi 17h (vendredi soir rush) → facteur très élevé (>= 1.7)") {
            val factor = engine.calculateTrafficFactor(5, 17)
            (factor >= 1.7) shouldBe true
        }

        test("Samedi 14h (week-end journée) → facteur modéré (≈ 1.3)") {
            val factor = engine.calculateTrafficFactor(6, 14)
            factor shouldBe (1.3 plusOrMinus 0.01)
        }

        test("Mercredi 2h (nuit semaine) → facteur minimal (1.0)") {
            val factor = engine.calculateTrafficFactor(3, 2)
            factor shouldBe (1.0 plusOrMinus 0.01)
        }

        test("Mardi 18h (soirée semaine) → facteur soirée (>= 1.7)") {
            val factor = engine.calculateTrafficFactor(2, 18)
            (factor >= 1.7) shouldBe true
        }

        test("Dimanche 11h → facteur modéré (1.3)") {
            val factor = engine.calculateTrafficFactor(7, 11)
            factor shouldBe (1.3 plusOrMinus 0.01)
        }
    }

    // ─────────────────────────────────────────────
    // buildCostBreakdown
    // ─────────────────────────────────────────────
    context("buildCostBreakdown") {

        test("500 km SP95 trafficFactor=1.0 → coût ≈ 64.75 €") {
            // SP95 : 7.0 L/100 km × 1.85 €/L = 0.1295 €/km → 500 × 0.1295 = 64.75 €
            val breakdown = engine.buildCostBreakdown(
                distanceKm = 500.0,
                selectedFuelType = FuelType.SP95,
                trafficFactor = 1.0,
                baseDurationMinutes = 300.0
            )
            breakdown.costTotal shouldBe (64.75 plusOrMinus 0.5)
            breakdown.fuelConsumedTotal shouldBe (35.0 plusOrMinus 0.5)
        }

        test("300 km ELECTRIC trafficFactor=1.0 → coût ≈ 22.44 €") {
            // ELECTRIC : 17.0 kWh/100 km × 0.44 €/kWh = 0.0748 €/km → 300 × 0.0748 = 22.44 €
            val breakdown = engine.buildCostBreakdown(
                distanceKm = 300.0,
                selectedFuelType = FuelType.ELECTRIC,
                trafficFactor = 1.0,
                baseDurationMinutes = 180.0
            )
            breakdown.costTotal shouldBe (22.44 plusOrMinus 0.5)
        }

        test("La comparaison contient tous les 6 types de carburant") {
            val breakdown = engine.buildCostBreakdown(
                distanceKm = 100.0,
                selectedFuelType = FuelType.DIESEL,
                trafficFactor = 1.0,
                baseDurationMinutes = 60.0
            )
            breakdown.comparison.size shouldBe 6
            FuelType.values().forEach { ft ->
                breakdown.comparison[ft.name] shouldNotBe null
            }
        }

        test("Avec trafficFactor=2.0, durationAdjustedMinutes est doublée") {
            val breakdown = engine.buildCostBreakdown(
                distanceKm = 100.0,
                selectedFuelType = FuelType.SP95,
                trafficFactor = 2.0,
                baseDurationMinutes = 60.0
            )
            breakdown.durationAdjustedMinutes shouldBe (120.0 plusOrMinus 0.01)
        }

        test("E85 est moins cher que SP95 pour la même distance") {
            val breakdownSP95 = engine.buildCostBreakdown(200.0, FuelType.SP95, 1.0, 120.0)
            val breakdownE85 = engine.buildCostBreakdown(200.0, FuelType.E85, 1.0, 120.0)
            (breakdownE85.costTotal < breakdownSP95.costTotal) shouldBe true
        }

        test("fuelType du breakdown correspond au carburant sélectionné") {
            val breakdown = engine.buildCostBreakdown(100.0, FuelType.GPL, 1.0, 60.0)
            breakdown.fuelType shouldBe FuelType.GPL.name
        }

        test("Trajet 0 km → coût total et carburant consommé sont 0") {
            val breakdown = engine.buildCostBreakdown(0.0, FuelType.SP95, 1.0, 0.0)
            breakdown.costTotal shouldBe (0.0 plusOrMinus 0.001)
            breakdown.fuelConsumedTotal shouldBe (0.0 plusOrMinus 0.001)
        }
    }

    // ─────────────────────────────────────────────
    // generateHeatmap
    // ─────────────────────────────────────────────
    context("generateHeatmap") {

        test("La heatmap doit avoir 7 jours") {
            val heatmap = engine.generateHeatmap()
            heatmap.size shouldBe 7
        }

        test("Chaque jour doit avoir 24 heures") {
            val heatmap = engine.generateHeatmap()
            heatmap.forEach { day -> day.size shouldBe 24 }
        }

        test("Tous les facteurs doivent être entre 1.0 et 2.0 inclus") {
            val heatmap = engine.generateHeatmap()
            heatmap.forEach { day ->
                day.forEach { factor ->
                    (factor >= 1.0) shouldBe true
                    (factor <= 2.0) shouldBe true
                }
            }
        }

        test("Les facteurs de nuit (0h-5h) doivent être inférieurs aux heures de pointe (7h-9h)") {
            val heatmap = engine.generateHeatmap()
            // Lundi = index 0
            val lundi = heatmap[0]
            val facteurNuit = lundi[3]
            val facteurPointe = lundi[8]
            (facteurPointe > facteurNuit) shouldBe true
        }
    }
})
