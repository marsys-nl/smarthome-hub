package network.marsys.smarthome.hub.feature.entity.domain.event

import network.marsys.smarthome.domain.entity.EntityIdentifier
import kotlin.time.Instant

sealed interface Event {
    val identifier: EntityIdentifier
    val occurredAt: Instant
}
