package network.marsys.smarthome.hub.feature.entity.infrastructure

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import network.marsys.smarthome.domain.entity.Entity
import network.marsys.smarthome.domain.entity.EntityIdentifier
import network.marsys.smarthome.hub.feature.entity.application.ports.inbound.FindEntity
import network.marsys.smarthome.hub.feature.entity.application.ports.inbound.GetEntities
import network.marsys.smarthome.hub.feature.entity.application.ports.outbound.EventStore
import network.marsys.smarthome.hub.feature.entity.domain.EntityAggregate
import network.marsys.smarthome.hub.feature.entity.domain.event.Event

class InMemoryEventStore : EventStore, FindEntity, GetEntities {
    private val mutex = Mutex()
    private val events = mutableMapOf<EntityIdentifier, MutableList<Event>>()

    override suspend fun append(vararg events: Event) =
        mutex.withLock {
            events.forEach { event ->
                this.events
                    .getOrPut(
                        key = event.identifier,
                        defaultValue = ::mutableListOf,
                    )
                    .add(event)
            }
        }

    private suspend fun append(event: Event): Unit =
        append(events = arrayOf(event))

    override suspend fun load(entity: EntityIdentifier): Collection<Event> =
        mutex.withLock {
            events.getOrDefault(entity, emptyList()).toList()
        }

    override suspend fun invoke(identifier: EntityIdentifier): Entity? =
        mutex.withLock {
            events[identifier]
                ?.let(::EntityAggregate)
                ?.let(EntityAggregate::entity)
        }

    override suspend fun invoke(): List<Entity> =
        mutex.withLock {
            events.values
                .map(::EntityAggregate)
                .map(EntityAggregate::entity)
        }
}
