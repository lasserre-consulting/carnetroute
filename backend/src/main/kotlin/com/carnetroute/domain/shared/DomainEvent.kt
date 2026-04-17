package com.carnetroute.domain.shared

import kotlinx.datetime.Instant
import java.util.UUID

abstract class DomainEvent {
    abstract val aggregateId: UUID
    abstract val aggregateType: String
    abstract val occurredAt: Instant
}
