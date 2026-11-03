package network.marsys.smarthome.hub.feature.entity.domain.event

import network.marsys.smarthome.domain.entity.Entity
import network.marsys.smarthome.domain.entity.EntityIdentifier
import kotlin.time.Clock
import kotlin.time.Instant

data class EntityDiscovered(
    override val identifier: EntityIdentifier,
    val state: Entity.State,
    override val occurredAt: Instant = Clock.System.now(),
) : Event
