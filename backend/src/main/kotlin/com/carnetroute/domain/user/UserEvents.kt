package com.carnetroute.domain.user

import com.carnetroute.domain.shared.DomainEvent
import kotlinx.datetime.Instant
import java.util.UUID

data class UserCreatedEvent(
    override val aggregateId: UUID,
    val email: String,
    val name: String,
    override val occurredAt: Instant
) : DomainEvent() {
    override val aggregateType: String = "User"
}

data class UserProfileUpdatedEvent(
    override val aggregateId: UUID,
    val name: String,
    override val occurredAt: Instant
) : DomainEvent() {
    override val aggregateType: String = "User"
}

data class UserDeletedEvent(
    override val aggregateId: UUID,
    override val occurredAt: Instant
) : DomainEvent() {
    override val aggregateType: String = "User"
}
