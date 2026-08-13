package network.marsys.smarthome.hub.feature.entity.application.event

import network.marsys.smarthome.domain.identifiers.EntityIdentifier
import kotlin.time.Instant

internal sealed interface Event {
    val identifier: EntityIdentifier
    val occurredAt: Instant
}
