package com.carnetroute.services

import com.carnetroute.models.Coordinates
import com.carnetroute.models.SimulationRequest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SimulationServiceTest {

    private val service = SimulationService()

    @Test
    fun `haversine distance Paris to Toulouse is roughly 588 km`() {
        val dist = service.haversineKm(48.8566, 2.3522, 43.6047, 1.4442)
        assertTrue(dist > 580 && dist < 600, "Expected ~588 km, got $dist")
    }

    @Test
    fun `road distance applies 1_35 factor`() {
        val paris = Coordinates(48.8566, 2.3522)
        val toulouse = Coordinates(43.6047, 1.4442)
        val straight = service.haversineKm(paris.lat, paris.lng, toulouse.lat, toulouse.lng)
        val road = service.roadDistanceKm(paris, toulouse)
        assertEquals(straight * 1.35, road, 0.1)
    }

    @Test
    fun `traffic factor at Monday 8am is peak`() {
        val factor = service.getTrafficFactor(0, 8) // Monday 8am
        assertTrue(factor >= 1.9, "Monday 8am should be near peak, got $factor")
    }

    @Test
    fun `traffic factor at Sunday 3am is minimal`() {
        val factor = service.getTrafficFactor(6, 3) // Sunday 3am
        assertTrue(factor < 1.1, "Sunday 3am should be minimal, got $factor")
    }

    @Test
    fun `friday evening has boost`() {
        val fridayEvening = service.getTrafficFactor(4, 17) // Friday 5pm
        val wednesdayEvening = service.getTrafficFactor(2, 17) // Wednesday 5pm
        assertTrue(fridayEvening > wednesdayEvening, "Friday evening should have boost")
    }

    @Test
    fun `simulation returns valid result`() {
        val request = SimulationRequest(
            from = Coordinates(48.8566, 2.3522, "Paris"),
            to = Coordinates(43.6047, 1.4442, "Toulouse"),
            fuelType = "sp95",
            trafficMode = "manual",
            manualTrafficLevel = 0,
            departureDay = 0,
            departureHour = 8
        )

        val result = service.simulate(request)

        assertTrue(result.distanceKm > 700, "Distance should be >700km road")
        assertTrue(result.baseTimeMin > 0)
        assertTrue(result.totalCost > 0)
        assertEquals(6, result.comparison.size, "Should have 6 fuel comparisons")
    }

    @Test
    fun `electric is cheaper per km than diesel`() {
        val request = SimulationRequest(
            from = Coordinates(48.8566, 2.3522),
            to = Coordinates(43.6047, 1.4442),
            fuelType = "diesel",
            trafficMode = "manual",
            manualTrafficLevel = 0,
            departureDay = 0,
            departureHour = 8
        )

        val result = service.simulate(request)
        val elecCost = result.comparison.first { it.key == "electrique" }.cost
        val dieselCost = result.comparison.first { it.key == "diesel" }.cost

        assertTrue(elecCost < dieselCost, "Electric should be cheaper, elec=$elecCost diesel=$dieselCost")
    }

    @Test
    fun `heatmap generates 7x24 grid`() {
        val heatmap = service.generateHeatmap(
            from = Coordinates(48.8566, 2.3522),
            to = Coordinates(43.6047, 1.4442),
            fuelType = "sp95"
        )

        assertEquals(7, heatmap.grid.size, "Should have 7 days")
        heatmap.grid.forEach { day ->
            assertEquals(24, day.size, "Each day should have 24 hours")
        }
        assertTrue(heatmap.baseTimeMin > 0)
        assertTrue(heatmap.distanceKm > 0)
    }

    @Test
    fun `all fuel profiles are accessible`() {
        val profiles = SimulationService.FUEL_PROFILES
        assertEquals(6, profiles.size)
        assertTrue(profiles.containsKey("sp95"))
        assertTrue(profiles.containsKey("sp98"))
        assertTrue(profiles.containsKey("diesel"))
        assertTrue(profiles.containsKey("e85"))
        assertTrue(profiles.containsKey("gpl"))
        assertTrue(profiles.containsKey("electrique"))
    }
}
