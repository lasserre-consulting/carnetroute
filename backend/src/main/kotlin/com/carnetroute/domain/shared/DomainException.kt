package com.carnetroute.domain.shared

import java.util.UUID

sealed class DomainException(message: String) : RuntimeException(message) {
    data class UserNotFound(val userId: UUID) : DomainException("User not found: $userId")
    data class UserAlreadyExists(val email: String) : DomainException("User already exists: $email")
    object InvalidCredentials : DomainException("Invalid email or password")
    data class VehicleNotFound(val vehicleId: UUID) : DomainException("Vehicle not found: $vehicleId")
    data class SimulationNotFound(val simulationId: UUID) : DomainException("Simulation not found: $simulationId")
    data class InvalidFuelType(val type: String) : DomainException("Invalid fuel type: $type")
    data class RoutingError(override val message: String) : DomainException(message)
    data class ValidationError(val field: String, override val message: String) : DomainException("$field: $message")
    data class Unauthorized(override val message: String = "Unauthorized") : DomainException(message)
}
