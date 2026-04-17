package com.carnetroute.application.history

import com.carnetroute.domain.history.HistoryRepository
import com.carnetroute.domain.history.JourneyHistory
import com.carnetroute.domain.shared.Page

class GetJourneyHistoryUseCase(
    private val historyRepository: HistoryRepository
) {
    suspend fun execute(userId: String, page: Int = 0, size: Int = 20): Page<JourneyHistory> {
        return historyRepository.findByUserId(userId, page, size)
    }
}
