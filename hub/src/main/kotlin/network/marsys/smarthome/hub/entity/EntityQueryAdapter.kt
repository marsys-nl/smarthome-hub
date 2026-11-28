package network.marsys.smarthome.hub.entity

import network.marsys.smarthome.domain.entity.Entity
import network.marsys.smarthome.domain.entity.EntityIdentifier
import network.marsys.smarthome.hub.feature.entity.application.ports.inbound.FindEntity
import network.marsys.smarthome.hub.feature.integration.application.ports.outbound.EntityQuery

class EntityQueryAdapter(
    private val finder: FindEntity,
) : EntityQuery {
    override suspend fun findByIdentifier(identifier: EntityIdentifier): Entity? =
        finder.invoke(identifier = identifier)
}
