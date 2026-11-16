package network.marsys.smarthome.hub.feature.entity.domain.event

import network.marsys.smarthome.domain.entity.Entity
import network.marsys.smarthome.domain.entity.EntityIdentifier
import kotlin.time.Instant

data class EntityActionAccepted(
    val action: Entity.Action,
    override val occurredAt: Instant,
) : Event {
    override val identifier: EntityIdentifier
        get() = action.entity
}
