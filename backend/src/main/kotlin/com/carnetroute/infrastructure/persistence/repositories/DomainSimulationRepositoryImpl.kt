package com.carnetroute.infrastructure.persistence.repositories

import com.carnetroute.domain.shared.Page
import com.carnetroute.domain.simulation.Simulation
import com.carnetroute.domain.simulation.SimulationRepository
import com.carnetroute.domain.simulation.vo.Coordinates
import com.carnetroute.domain.simulation.vo.CostBreakdown
import com.carnetroute.domain.simulation.vo.Route
import com.carnetroute.domain.simulation.vo.TrafficConditions
import com.carnetroute.infrastructure.persistence.DatabaseFactory
import com.carnetroute.infrastructure.persistence.dbQuery
import com.carnetroute.infrastructure.persistence.tables.SimulationsTable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq

/**
 * Implémentation de [domain.simulation.SimulationRepository] — stocke et
 * recharge les simulations dans la table `simulations`.
 *
 * La route complète est sérialisée en JSON dans la colonne `comparison_json`
 * pour préserver le géométrie polyline.
 */
class DomainSimulationRepositoryImpl(
    @Suppress("UNUSED_PARAMETER") private val databaseFactory: DatabaseFactory,
) : SimulationRepository {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    override suspend fun save(simulation: Simulation): Simulation = dbQuery {
        val routeJson = json.encodeToString(simulation.route)
        val costsJson = json.encodeToString(simulation.costs)

        SimulationsTable.insertIgnore {
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
            it[geometry] = routeJson
            it[trafficMode] = simulation.traffic.mode
            it[trafficFactor] = simulation.traffic.factor
            it[fuelType] = simulation.costs.fuelType
            it[pricePerUnit] = simulation.costs.pricePerUnit
            it[consumptionPer100km] = simulation.costs.consumptionPer100km
            it[fuelConsumedTotal] = simulation.costs.fuelConsumedTotal
            it[costTotal] = simulation.costs.costTotal
            it[durationAdjustedMinutes] = simulation.costs.durationAdjustedMinutes
            it[comparisonJson] = costsJson
            it[createdAt] = simulation.createdAt
        }
        simulation
    }

    override suspend fun findById(id: String): Simulation? = dbQuery {
        SimulationsTable.selectAll().where { SimulationsTable.id eq id }
            .singleOrNull()
            ?.toSimulation()
    }

    override suspend fun findByUserId(userId: String, page: Int, size: Int): Page<Simulation> = dbQuery {
        val total = SimulationsTable.selectAll().where { SimulationsTable.userId eq userId }.count()

        val content = SimulationsTable
            .selectAll().where { SimulationsTable.userId eq userId }
            .orderBy(SimulationsTable.createdAt, SortOrder.DESC)
            .limit(size).offset((page * size).toLong())
            .map { it.toSimulation() }

        Page(content = content, totalElements = total, page = page, size = size)
    }

    private fun ResultRow.toSimulation(): Simulation {
        val costsBreakdown: CostBreakdown = try {
            json.decodeFromString(this[SimulationsTable.comparisonJson])
        } catch (e: Exception) {
            // Reconstruction minimale si le JSON est invalide
            CostBreakdown(
                fuelType = this[SimulationsTable.fuelType],
                pricePerUnit = this[SimulationsTable.pricePerUnit],
                consumptionPer100km = this[SimulationsTable.consumptionPer100km],
                fuelConsumedTotal = this[SimulationsTable.fuelConsumedTotal],
                costTotal = this[SimulationsTable.costTotal],
                durationAdjustedMinutes = this[SimulationsTable.durationAdjustedMinutes],
                comparison = emptyMap(),
            )
        }

        val geometry: List<List<Double>> = try {
            val route = json.decodeFromString<Route>(this[SimulationsTable.geometry] ?: "")
            route.geometry
        } catch (e: Exception) {
            emptyList()
        }

        return Simulation(
            id = this[SimulationsTable.id],
            userId = this[SimulationsTable.userId],
            vehicleId = this[SimulationsTable.vehicleId],
            route = Route(
                from = Coordinates(
                    lat = this[SimulationsTable.fromLat],
                    lng = this[SimulationsTable.fromLng],
                    label = this[SimulationsTable.fromLabel],
                ),
                to = Coordinates(
                    lat = this[SimulationsTable.toLat],
                    lng = this[SimulationsTable.toLng],
                    label = this[SimulationsTable.toLabel],
                ),
                distanceKm = this[SimulationsTable.distanceKm],
                durationMinutes = this[SimulationsTable.durationMinutes],
                geometry = geometry,
            ),
            traffic = TrafficConditions(
                mode = this[SimulationsTable.trafficMode],
                factor = this[SimulationsTable.trafficFactor],
            ),
            costs = costsBreakdown,
            createdAt = this[SimulationsTable.createdAt],
        )
    }
}
