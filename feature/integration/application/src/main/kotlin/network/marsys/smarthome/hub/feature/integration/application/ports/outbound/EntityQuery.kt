package network.marsys.smarthome.hub.feature.integration.application.ports.outbound

import network.marsys.smarthome.domain.entity.Entity
import network.marsys.smarthome.domain.entity.EntityIdentifier

fun interface EntityQuery {
    suspend fun findByIdentifier(identifier: EntityIdentifier): Entity?
}
