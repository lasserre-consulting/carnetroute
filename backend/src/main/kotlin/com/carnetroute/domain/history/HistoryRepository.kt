package com.carnetroute.domain.history

import com.carnetroute.domain.shared.Page

interface HistoryRepository {
    suspend fun save(journeyHistory: JourneyHistory): JourneyHistory
    suspend fun findByUserId(userId: String, page: Int, size: Int): Page<JourneyHistory>
    suspend fun countByUserId(userId: String): Long
}
