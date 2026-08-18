package network.marsys.smarthome.hub.feature.integration.infrastructure

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import network.marsys.smarthome.domain.identifiers.EntityIdentifier
import network.marsys.smarthome.hub.feature.entity.domain.event.Event
import network.marsys.smarthome.hub.feature.integration.application.ports.outbound.EventStore

class InMemoryEventStore : EventStore {
    private val mutex = Mutex()
    private val events = mutableMapOf<EntityIdentifier, MutableList<Event>>()

    override suspend fun append(vararg events: Event) =
        events.forEach { event ->
            append(event = event)
        }

    private suspend fun append(event: Event): Unit =
        mutex.withLock {
            events.getOrPut(key = event.identifier, defaultValue = ::mutableListOf)
                .add(event)
        }

    override suspend fun load(entity: EntityIdentifier): Collection<Event> =
        mutex.withLock {
            events.getOrDefault(entity, emptyList()).toList()
        }
}
