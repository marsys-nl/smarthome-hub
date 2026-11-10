package network.marsys.smarthome.hub.feature.entity.infrastructure

import network.marsys.smarthome.domain.capability.with
import network.marsys.smarthome.domain.entity.Entity
import network.marsys.smarthome.domain.entity.formattedName
import network.marsys.smarthome.hub.feature.entity.application.ports.outbound.EntityUpdatePublisher
import network.marsys.smarthome.hub.feature.entity.domain.event.CapabilityUpdated
import network.marsys.smarthome.hub.feature.entity.domain.event.EntityBecameUnavailable
import network.marsys.smarthome.hub.feature.entity.domain.event.EntityDiscovered
import network.marsys.smarthome.hub.feature.entity.domain.event.EntityProvisioned
import network.marsys.smarthome.hub.feature.entity.domain.event.Event
import network.marsys.smarthome.hub.feature.platform.application.PlatformEvent
import network.marsys.smarthome.hub.feature.platform.application.PlatformEventPublisher

class PlatformEntityUpdatePublisher(
    private val publisher: PlatformEventPublisher,
) : EntityUpdatePublisher {
    @Suppress("ktlint:standard:trailing-comma-on-declaration-site")
    override suspend fun publish(event: Event, entity: Entity) =
        when (event) {
            is CapabilityUpdated -> publish(event = event, entity = entity)
            else -> Unit
        }

    private suspend fun publish(event: CapabilityUpdated, entity: Entity) {
        val state = entity.state as? Entity.State.Known
            ?: error("Entity state is not known for entity '${entity.identifier}'")

        val capability = state.get(event.capability.type with event.capability.context)
            ?: error("Updated capability '${event.capability::class.formattedName}' not found")

        publisher.publish(
            event = PlatformEvent.CapabilityUpdated(
                identifier = entity.identifier,
                capability = capability,
                occurredAt = event.occurredAt,
            ),
        )
    }
}
