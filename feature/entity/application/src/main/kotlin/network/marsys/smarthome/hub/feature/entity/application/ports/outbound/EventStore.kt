package network.marsys.smarthome.hub.feature.entity.application.ports.outbound

import network.marsys.smarthome.domain.entity.EntityIdentifier
import network.marsys.smarthome.hub.feature.entity.domain.event.Event

interface EventStore {
    suspend fun append(vararg events: Event)
    suspend fun load(entity: EntityIdentifier): Collection<Event>
}
