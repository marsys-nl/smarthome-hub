package network.marsys.smarthome.hub.feature.entity.application.ports.outbound

import network.marsys.smarthome.hub.feature.entity.domain.entity.Entity

fun interface EntityUpdatePublisher {
    suspend fun publish(entity: Entity)
}
