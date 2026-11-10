package network.marsys.smarthome.hub.feature.entity.application

import network.marsys.smarthome.domain.entity.Entity
import network.marsys.smarthome.hub.feature.entity.application.ports.outbound.EntityUpdatePublisher
import network.marsys.smarthome.hub.feature.entity.domain.event.Event

internal class RecordingEntityUpdatePublisher : EntityUpdatePublisher {
    val updates: List<Pair<Event, Entity>>
        field = mutableListOf()

    override suspend fun publish(
        event: Event,
        entity: Entity,
    ) {
        updates += event to entity
    }
}
