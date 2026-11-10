package network.marsys.smarthome.hub.feature.entity.application.ports.outbound

import network.marsys.smarthome.domain.entity.Entity
import network.marsys.smarthome.hub.feature.entity.domain.event.Event

fun interface EntityUpdatePublisher {
    suspend fun publish(event: Event, entity: Entity)
}
