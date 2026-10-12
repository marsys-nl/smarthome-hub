package network.marsys.smarthome.hub.feature.entity.domain.event

import network.marsys.smarthome.domain.identifiers.EntityIdentifier
import network.marsys.smarthome.hub.feature.entity.domain.entity.Entity
import network.marsys.smarthome.hub.feature.entity.domain.entity.EntityNaming
import kotlin.time.Clock
import kotlin.time.Instant

data class EntityProvisioned(
    override val identifier: EntityIdentifier,
    val type: Entity.Type<*>,
    val naming: EntityNaming,
    override val occurredAt: Instant = Clock.System.now(),
) : Event
