package network.marsys.smarthome.hub.feature.integration.application.ports.outbound

import network.marsys.smarthome.domain.identifiers.EntityIdentifier
import network.marsys.smarthome.hub.feature.entity.domain.event.Event

interface EventStore {
    suspend fun append(vararg events: Event)
    suspend fun load(entity: EntityIdentifier): Collection<Event>
}
