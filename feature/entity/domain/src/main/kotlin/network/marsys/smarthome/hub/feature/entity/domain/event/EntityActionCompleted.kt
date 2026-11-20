package network.marsys.smarthome.hub.feature.entity.domain.event

import network.marsys.smarthome.domain.entity.EntityIdentifier
import kotlin.time.Instant

data class EntityActionCompleted(
    val accepted: EntityActionAccepted,
    override val occurredAt: Instant,
) : EntityActionEvent {
    override val identifier: EntityIdentifier
        get() = accepted.action.entity
}
