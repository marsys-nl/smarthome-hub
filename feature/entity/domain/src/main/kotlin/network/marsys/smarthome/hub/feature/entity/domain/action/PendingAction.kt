package network.marsys.smarthome.hub.feature.entity.domain.action

import network.marsys.smarthome.domain.entity.Entity
import kotlin.time.Instant

data class PendingAction(
    val action: Entity.Action,
    val acceptedAt: Instant,
)
