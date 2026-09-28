package network.marsys.smarthome.hub.feature.entity.application.ports.outbound

import network.marsys.smarthome.hub.feature.entity.domain.event.Event

fun interface EntityEventPublisher {
    suspend fun publish(event: Event)
}
