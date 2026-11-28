package network.marsys.smarthome.hub.feature.entity.application.ports.inbound

import network.marsys.smarthome.domain.entity.Entity
import network.marsys.smarthome.domain.entity.EntityIdentifier

fun interface FindEntity {
    suspend operator fun invoke(identifier: EntityIdentifier): Entity?
}
