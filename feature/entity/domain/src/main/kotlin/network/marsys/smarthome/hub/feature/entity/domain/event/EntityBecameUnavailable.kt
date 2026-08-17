package network.marsys.smarthome.hub.feature.entity.domain.event

import network.marsys.smarthome.domain.identifiers.EntityIdentifier
import kotlin.time.Clock
import kotlin.time.Instant

data class EntityBecameUnavailable(
    override val identifier: EntityIdentifier,
    override val occurredAt: Instant = Clock.System.now(),
) : Event
