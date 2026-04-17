package com.carnetroute.application.history

import com.carnetroute.domain.history.HistoryRepository
import com.carnetroute.domain.history.MonthlyStats
import com.carnetroute.domain.history.UserStatistics
import com.carnetroute.infrastructure.persistence.repositories.DomainHistoryRepositoryImpl

private const val CO2_KG_PER_KM_BASELINE = 0.120

class GetUserStatisticsUseCase(
    private val historyRepository: HistoryRepository,
) {
    suspend fun execute(userId: String): UserStatistics {
        // Utilise findAllByUserId si le repo le supporte, sinon pagine complètement
        val journeys = if (historyRepository is DomainHistoryRepositoryImpl) {
            historyRepository.findAllByUserId(userId)
        } else {
            // Fallback : première page de grande taille
            historyRepository.findByUserId(userId, 0, Int.MAX_VALUE).content
        }

        val totalJourneys = journeys.size
        val totalDistanceKm = journeys.sumOf { it.distanceKm }
        val totalCostEur = journeys.sumOf { it.costTotal }
        val totalDurationMinutes = journeys.sumOf { it.durationMinutes }
        val carbonEmissionKg = totalDistanceKm * CO2_KG_PER_KM_BASELINE

        val mostUsedFuelType = journeys
            .groupingBy { it.fuelType }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key

        val monthlyStats = journeys
            .groupBy { journey -> journey.createdAt.take(7) } // "YYYY-MM"
            .mapValues { (month, monthJourneys) ->
                MonthlyStats(
                    month = month,
                    journeys = monthJourneys.size,
                    distanceKm = monthJourneys.sumOf { it.distanceKm },
                    costEur = monthJourneys.sumOf { it.costTotal },
                )
            }

        return UserStatistics(
            userId = userId,
            totalJourneys = totalJourneys,
            totalDistanceKm = totalDistanceKm,
            totalCostEur = totalCostEur,
            totalDurationMinutes = totalDurationMinutes,
            carbonEmissionKg = carbonEmissionKg,
            mostUsedFuelType = mostUsedFuelType,
            monthlyStats = monthlyStats,
        )
    }
}
