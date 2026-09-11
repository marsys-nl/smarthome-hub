package network.marsys.smarthome.hub.core.eventstore.infrastructure

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import network.marsys.smarthome.domain.identifiers.EntityIdentifier
import network.marsys.smarthome.hub.core.eventstore.application.ports.outbound.EventStore
import network.marsys.smarthome.hub.feature.entity.domain.event.Event
import kotlin.collections.getOrDefault

class InMemoryEventStore : EventStore {
    private val mutex = Mutex()
    private val events = mutableMapOf<EntityIdentifier, MutableList<Event>>()

    override suspend fun append(vararg events: Event) =
        events.forEach { event ->
            append(event = event)
        }

    override suspend fun identifiers(): Collection<EntityIdentifier> =
        events.keys

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
