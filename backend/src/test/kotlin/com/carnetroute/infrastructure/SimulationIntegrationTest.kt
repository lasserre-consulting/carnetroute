package com.carnetroute.infrastructure

import com.carnetroute.domain.simulation.Simulation
import com.carnetroute.domain.simulation.SimulationEngine
import com.carnetroute.domain.simulation.vo.Coordinates
import com.carnetroute.domain.simulation.vo.TrafficConditions
import com.carnetroute.domain.vehicle.vo.FuelType
import com.carnetroute.infrastructure.routing.HaversineCalculator
import com.carnetroute.infrastructure.persistence.tables.SimulationsTable
import com.carnetroute.infrastructure.persistence.tables.UsersTable
import com.carnetroute.infrastructure.persistence.tables.VehiclesTable
import com.carnetroute.infrastructure.persistence.tables.JourneyHistoriesTable
import com.carnetroute.infrastructure.persistence.tables.FuelPricesTable
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import org.testcontainers.containers.PostgreSQLContainer

/**
 * Test d'intégration Testcontainers.
 *
 * Vérifie que :
 * 1. La base PostgreSQL démarre correctement et les tables peuvent être créées.
 * 2. Le moteur de simulation calcule des résultats cohérents end-to-end.
 * 3. La persistance d'une simulation via SimulationsTable est fonctionnelle.
 * 4. La persistance d'un utilisateur via UsersTable est fonctionnelle.
 *
 * Note : SimulationRepositoryImpl n'existe pas encore dans l'infrastructure.
 * Ce test accède directement à SimulationsTable (Exposed) pour valider la couche
 * de persistance sans dépendre d'une implémentation non encore créée.
 */
class SimulationIntegrationTest : FunSpec({

    val postgres = PostgreSQLContainer<Nothing>("postgres:16-alpine").apply {
        withDatabaseName("carnetroute_test")
        withUsername("test")
        withPassword("test")
    }

    val json = Json { ignoreUnknownKeys = true }

    // ─────────────────────────────────────────────
    // Cycle de vie du conteneur
    // ─────────────────────────────────────────────
    beforeSpec {
        postgres.start()

        Database.connect(
            url = postgres.jdbcUrl,
            driver = "org.postgresql.Driver",
            user = postgres.username,
            password = postgres.password
        )

        transaction {
            SchemaUtils.create(
                UsersTable,
                VehiclesTable,
                SimulationsTable,
                JourneyHistoriesTable,
                FuelPricesTable,
            )
        }
    }

    afterSpec {
        postgres.stop()
    }

    // ─────────────────────────────────────────────
    // Connectivité & schéma
    // ─────────────────────────────────────────────
    context("infrastructure / base de données") {

        test("le conteneur PostgreSQL doit être démarré et accessible") {
            postgres.isRunning shouldBe true
        }

        test("les tables doivent exister après SchemaUtils.create") {
            // Une simple requête COUNT ne doit pas lever d'exception
            val count = newSuspendedTransaction(Dispatchers.IO) {
                SimulationsTable.selectAll().count()
            }
            count shouldBe 0L
        }
    }

    // ─────────────────────────────────────────────
    // Moteur de simulation end-to-end
    // ─────────────────────────────────────────────
    context("SimulationEngine end-to-end (calculs)") {

        test("Paris → Toulouse : distance > 0 et coût > 0") {
            val engine = SimulationEngine()
            val from = Coordinates(48.8566, 2.3522, "Paris")
            val to = Coordinates(43.6047, 1.4442, "Toulouse")
            val route = HaversineCalculator.getRoute(from, to)
            val breakdown = engine.buildCostBreakdown(
                distanceKm = route.distanceKm,
                selectedFuelType = FuelType.SP95,
                trafficFactor = 1.0,
                baseDurationMinutes = route.durationMinutes
            )

            (route.distanceKm > 0.0) shouldBe true
            (breakdown.costTotal > 0.0) shouldBe true
            breakdown.comparison.size shouldBe 6
        }

        test("HaversineCalculator.getRoute retourne une route cohérente Paris → Lyon") {
            val from = Coordinates(48.8566, 2.3522, "Paris")
            val to = Coordinates(45.7640, 4.8357, "Lyon")
            val route = HaversineCalculator.getRoute(from, to)

            (route.distanceKm > 350.0) shouldBe true
            (route.distanceKm < 600.0) shouldBe true
            (route.durationMinutes > 0.0) shouldBe true
        }
    }

    // ─────────────────────────────────────────────
    // Persistance d'une simulation (via SimulationsTable)
    // ─────────────────────────────────────────────
    context("persistance d'une simulation") {

        test("sauvegarder et récupérer une simulation via SimulationsTable") {
            val engine = SimulationEngine()
            val from = Coordinates(48.8566, 2.3522, "Paris")
            val to = Coordinates(43.6047, 1.4442, "Toulouse")
            val route = HaversineCalculator.getRoute(from, to)
            val breakdown = engine.buildCostBreakdown(
                distanceKm = route.distanceKm,
                selectedFuelType = FuelType.SP95,
                trafficFactor = 1.0,
                baseDurationMinutes = route.durationMinutes
            )

            val simulation = Simulation.create(
                userId = null,
                vehicleId = null,
                route = route,
                traffic = TrafficConditions(),
                costs = breakdown
            )

            // Persister
            newSuspendedTransaction(Dispatchers.IO) {
                SimulationsTable.insert {
                    it[id] = simulation.id
                    it[userId] = simulation.userId
                    it[vehicleId] = simulation.vehicleId
                    it[fromLat] = simulation.route.from.lat
                    it[fromLng] = simulation.route.from.lng
                    it[fromLabel] = simulation.route.from.label
                    it[toLat] = simulation.route.to.lat
                    it[toLng] = simulation.route.to.lng
                    it[toLabel] = simulation.route.to.label
                    it[distanceKm] = simulation.route.distanceKm
                    it[durationMinutes] = simulation.route.durationMinutes
                    it[geometry] = null
                    it[trafficMode] = simulation.traffic.mode
                    it[trafficFactor] = simulation.traffic.factor
                    it[fuelType] = simulation.costs.fuelType
                    it[pricePerUnit] = simulation.costs.pricePerUnit
                    it[consumptionPer100km] = simulation.costs.consumptionPer100km
                    it[fuelConsumedTotal] = simulation.costs.fuelConsumedTotal
                    it[costTotal] = simulation.costs.costTotal
                    it[durationAdjustedMinutes] = simulation.costs.durationAdjustedMinutes
                    it[comparisonJson] = json.encodeToString(simulation.costs.comparison)
                    it[createdAt] = simulation.createdAt
                }
            }

            // Récupérer
            val row = newSuspendedTransaction(Dispatchers.IO) {
                SimulationsTable.selectAll()
                    .where { SimulationsTable.id eq simulation.id }
                    .singleOrNull()
            }

            row shouldNotBe null
            row!![SimulationsTable.id] shouldBe simulation.id
            row[SimulationsTable.costTotal] shouldBe breakdown.costTotal
            row[SimulationsTable.fuelType] shouldBe "SP95"
            row[SimulationsTable.distanceKm] shouldBe route.distanceKm
        }

        test("deux simulations avec des IDs différents doivent être indépendantes") {
            val engine = SimulationEngine()
            val from = Coordinates(48.8566, 2.3522, "Paris")
            val to = Coordinates(44.8378, -0.5792, "Bordeaux")
            val route = HaversineCalculator.getRoute(from, to)

            val breakdown1 = engine.buildCostBreakdown(route.distanceKm, FuelType.DIESEL, 1.0, route.durationMinutes)
            val breakdown2 = engine.buildCostBreakdown(route.distanceKm, FuelType.ELECTRIC, 1.0, route.durationMinutes)

            val sim1 = Simulation.create(null, null, route, TrafficConditions(), breakdown1)
            val sim2 = Simulation.create(null, null, route, TrafficConditions(), breakdown2)

            newSuspendedTransaction(Dispatchers.IO) {
                listOf(sim1, sim2).forEach { sim ->
                    SimulationsTable.insert {
                        it[id] = sim.id
                        it[userId] = null
                        it[vehicleId] = null
                        it[fromLat] = sim.route.from.lat
                        it[fromLng] = sim.route.from.lng
                        it[fromLabel] = sim.route.from.label
                        it[toLat] = sim.route.to.lat
                        it[toLng] = sim.route.to.lng
                        it[toLabel] = sim.route.to.label
                        it[distanceKm] = sim.route.distanceKm
                        it[durationMinutes] = sim.route.durationMinutes
                        it[geometry] = null
                        it[trafficMode] = sim.traffic.mode
                        it[trafficFactor] = sim.traffic.factor
                        it[fuelType] = sim.costs.fuelType
                        it[pricePerUnit] = sim.costs.pricePerUnit
                        it[consumptionPer100km] = sim.costs.consumptionPer100km
                        it[fuelConsumedTotal] = sim.costs.fuelConsumedTotal
                        it[costTotal] = sim.costs.costTotal
                        it[durationAdjustedMinutes] = sim.costs.durationAdjustedMinutes
                        it[comparisonJson] = json.encodeToString(sim.costs.comparison)
                        it[createdAt] = sim.createdAt
                    }
                }
            }

            val foundSim1 = newSuspendedTransaction(Dispatchers.IO) {
                SimulationsTable.selectAll()
                    .where { SimulationsTable.id eq sim1.id }
                    .singleOrNull()
            }
            val foundSim2 = newSuspendedTransaction(Dispatchers.IO) {
                SimulationsTable.selectAll()
                    .where { SimulationsTable.id eq sim2.id }
                    .singleOrNull()
            }

            foundSim1 shouldNotBe null
            foundSim2 shouldNotBe null
            foundSim1!![SimulationsTable.fuelType] shouldBe "DIESEL"
            foundSim2!![SimulationsTable.fuelType] shouldBe "ELECTRIC"
            foundSim1[SimulationsTable.id] shouldBe sim1.id
            foundSim2[SimulationsTable.id] shouldBe sim2.id
        }
    }

    // ─────────────────────────────────────────────
    // Persistance d'un utilisateur (via UsersTable)
    // ─────────────────────────────────────────────
    context("persistance d'un utilisateur") {

        test("sauvegarder et retrouver un utilisateur via UsersTable") {
            val userId = "test-user-${System.currentTimeMillis()}"
            val email = "integration-$userId@example.com"

            newSuspendedTransaction(Dispatchers.IO) {
                UsersTable.insert {
                    it[id] = userId
                    it[UsersTable.email] = email
                    it[passwordHash] = "hashed_test"
                    it[name] = "Integration User"
                    it[preferences] = "{}"
                    it[createdAt] = "2026-01-01T00:00:00Z"
                    it[updatedAt] = "2026-01-01T00:00:00Z"
                }
            }

            val row = newSuspendedTransaction(Dispatchers.IO) {
                UsersTable.selectAll()
                    .where { UsersTable.id eq userId }
                    .singleOrNull()
            }

            row shouldNotBe null
            row!![UsersTable.id] shouldBe userId
            row[UsersTable.email] shouldBe email
            row[UsersTable.name] shouldBe "Integration User"
        }

        test("deux utilisateurs avec des emails différents peuvent coexister") {
            val ts = System.currentTimeMillis()
            val user1Id = "user-a-$ts"
            val user2Id = "user-b-$ts"

            newSuspendedTransaction(Dispatchers.IO) {
                listOf(
                    user1Id to "usera-$ts@example.com",
                    user2Id to "userb-$ts@example.com"
                ).forEach { (uid, mail) ->
                    UsersTable.insert {
                        it[id] = uid
                        it[email] = mail
                        it[passwordHash] = "hash"
                        it[name] = "User $uid"
                        it[preferences] = "{}"
                        it[createdAt] = "2026-01-01T00:00:00Z"
                        it[updatedAt] = "2026-01-01T00:00:00Z"
                    }
                }
            }

            val count = newSuspendedTransaction(Dispatchers.IO) {
                UsersTable.selectAll()
                    .where { UsersTable.id eq user1Id }
                    .count() +
                UsersTable.selectAll()
                    .where { UsersTable.id eq user2Id }
                    .count()
            }

            count shouldBe 2L
        }
    }
})
