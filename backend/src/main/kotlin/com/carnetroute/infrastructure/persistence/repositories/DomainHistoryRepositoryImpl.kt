package com.carnetroute.infrastructure.persistence.repositories

import com.carnetroute.domain.history.HistoryRepository
import com.carnetroute.domain.history.JourneyHistory
import com.carnetroute.domain.shared.Page
import com.carnetroute.infrastructure.persistence.DatabaseFactory
import com.carnetroute.infrastructure.persistence.dbQuery
import com.carnetroute.infrastructure.persistence.tables.JourneyHistoriesTable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq

/**
 * Implémentation de [HistoryRepository] — utilise la table `journey_history`
 * via Exposed avec des IDs String.
 */
class DomainHistoryRepositoryImpl(
    @Suppress("UNUSED_PARAMETER") private val databaseFactory: DatabaseFactory,
) : HistoryRepository {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    override suspend fun save(journeyHistory: JourneyHistory): JourneyHistory = dbQuery {
        JourneyHistoriesTable.insertIgnore {
            it[id] = journeyHistory.id
            it[userId] = journeyHistory.userId
            it[simulationId] = journeyHistory.simulationId
            it[fuelType] = journeyHistory.fuelType
            it[distanceKm] = journeyHistory.distanceKm
            it[costTotal] = journeyHistory.costTotal
            it[durationMinutes] = journeyHistory.durationMinutes
            it[carbonEmissionKg] = journeyHistory.carbonEmissionKg
            it[tags] = json.encodeToString(journeyHistory.tags)
            it[createdAt] = journeyHistory.createdAt
        }
        journeyHistory
    }

    override suspend fun findByUserId(userId: String, page: Int, size: Int): Page<JourneyHistory> = dbQuery {
        val total = JourneyHistoriesTable
            .selectAll().where { JourneyHistoriesTable.userId eq userId }
            .count()

        val content = JourneyHistoriesTable
            .selectAll().where { JourneyHistoriesTable.userId eq userId }
            .orderBy(JourneyHistoriesTable.createdAt, SortOrder.DESC)
            .limit(size).offset((page * size).toLong())
            .map { it.toJourneyHistory() }

        Page(content = content, totalElements = total, page = page, size = size)
    }

    override suspend fun countByUserId(userId: String): Long = dbQuery {
        JourneyHistoriesTable
            .selectAll().where { JourneyHistoriesTable.userId eq userId }
            .count()
    }

    /** Retourne TOUS les trajets d'un utilisateur (pour les statistiques agrégées). */
    suspend fun findAllByUserId(userId: String): List<JourneyHistory> = dbQuery {
        JourneyHistoriesTable
            .selectAll().where { JourneyHistoriesTable.userId eq userId }
            .orderBy(JourneyHistoriesTable.createdAt, SortOrder.DESC)
            .map { it.toJourneyHistory() }
    }

    private fun ResultRow.toJourneyHistory(): JourneyHistory {
        val tagsRaw = this[JourneyHistoriesTable.tags]
        val tags: List<String> = runCatching {
            json.decodeFromString<List<String>>(tagsRaw)
        }.getOrElse {
            // Fallback : parsing JSON array manuel simple
            tagsRaw.removeSurrounding("[", "]")
                .split(",")
                .mapNotNull { it.trim().removeSurrounding("\"").takeIf { s -> s.isNotEmpty() } }
        }

        return JourneyHistory(
            id = this[JourneyHistoriesTable.id],
            userId = this[JourneyHistoriesTable.userId],
            simulationId = this[JourneyHistoriesTable.simulationId],
            fuelType = this[JourneyHistoriesTable.fuelType],
            distanceKm = this[JourneyHistoriesTable.distanceKm],
            costTotal = this[JourneyHistoriesTable.costTotal],
            durationMinutes = this[JourneyHistoriesTable.durationMinutes],
            carbonEmissionKg = this[JourneyHistoriesTable.carbonEmissionKg],
            createdAt = this[JourneyHistoriesTable.createdAt],
            tags = tags,
        )
    }
}
