package com.carnetroute.application.simulation

import com.carnetroute.domain.shared.Page
import com.carnetroute.domain.simulation.Simulation
import com.carnetroute.domain.simulation.SimulationRepository

class GetSimulationHistoryUseCase(
    private val simulationRepository: SimulationRepository
) {
    suspend fun execute(userId: String, page: Int = 0, size: Int = 20): Page<Simulation> {
        return simulationRepository.findByUserId(userId, page, size)
    }
}
