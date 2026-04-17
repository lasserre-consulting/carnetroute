package com.carnetroute.domain.vehicle

import com.carnetroute.domain.shared.DomainEvent
import kotlinx.datetime.Instant
import java.util.UUID

data class VehicleCreatedEvent(
    override val aggregateId: UUID,
    override val occurredAt: Instant
) : DomainEvent() {
    override val aggregateType: String = "Vehicle"
}

data class VehicleUpdatedEvent(
    override val aggregateId: UUID,
    override val occurredAt: Instant
) : DomainEvent() {
    override val aggregateType: String = "Vehicle"
}

data class VehicleDeletedEvent(
    override val aggregateId: UUID,
    override val occurredAt: Instant
) : DomainEvent() {
    override val aggregateType: String = "Vehicle"
}
