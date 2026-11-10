package network.marsys.smarthome.hub.feature.platform.application

import network.marsys.smarthome.domain.capability.Capability
import network.marsys.smarthome.domain.entity.EntityIdentifier
import kotlin.time.Instant

sealed interface PlatformEvent {
    data class CapabilityUpdated(
        val identifier: EntityIdentifier,
        val capability: Capability<*, *>,
        val updatedAt: Instant,
    ) : PlatformEvent
}
