package com.carnetroute.domain.simulation

import com.carnetroute.domain.shared.Page

interface SimulationRepository {
    suspend fun save(simulation: Simulation): Simulation
    suspend fun findById(id: String): Simulation?
    suspend fun findByUserId(userId: String, page: Int, size: Int): Page<Simulation>
}
