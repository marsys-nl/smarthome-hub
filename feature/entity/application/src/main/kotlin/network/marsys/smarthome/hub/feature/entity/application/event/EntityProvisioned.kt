package network.marsys.smarthome.hub.feature.entity.application.event

import network.marsys.smarthome.domain.identifiers.EntityIdentifier
import network.marsys.smarthome.hub.feature.entity.domain.entity.Entity
import kotlin.time.Clock
import kotlin.time.Instant

internal data class EntityProvisioned(
    override val identifier: EntityIdentifier,
    val type: Entity.Type<*, *>,
    override val occurredAt: Instant = Clock.System.now(),
) : Event
