package network.marsys.smarthome.hub.feature.entity.domain.event

import network.marsys.smarthome.domain.identifiers.EntityIdentifier
import network.marsys.smarthome.hub.feature.entity.domain.capability.Capability
import kotlin.time.Clock
import kotlin.time.Instant

data class CapabilityUpdated(
    override val identifier: EntityIdentifier,
    val capability: Capability<*>,
    override val occurredAt: Instant = Clock.System.now(),
) : Event
