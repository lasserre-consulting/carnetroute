package com.carnetroute.application.simulation

import com.carnetroute.domain.simulation.SimulationEngine

data class HeatmapResult(val matrix: Array<DoubleArray>) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is HeatmapResult) return false
        return matrix.contentDeepEquals(other.matrix)
    }

    override fun hashCode(): Int = matrix.contentDeepHashCode()
}

class GenerateHeatmapUseCase(
    private val simulationEngine: SimulationEngine
) {
    suspend fun execute(): HeatmapResult {
        val matrix = simulationEngine.generateHeatmap()
        return HeatmapResult(matrix)
    }
}
