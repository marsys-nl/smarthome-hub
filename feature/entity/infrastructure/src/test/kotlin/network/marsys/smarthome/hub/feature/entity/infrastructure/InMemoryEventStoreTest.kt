package network.marsys.smarthome.hub.feature.entity.infrastructure

import de.infix.testBalloon.framework.core.testSuite
import dev.nmarsman.expect.api.expectThat
import dev.nmarsman.expect.assertions.all
import dev.nmarsman.expect.assertions.doesNotContain
import dev.nmarsman.expect.assertions.hasSize
import dev.nmarsman.expect.assertions.isA
import dev.nmarsman.expect.assertions.isEmpty
import dev.nmarsman.expect.assertions.isEqualTo
import dev.nmarsman.expect.assertions.isNull
import dev.nmarsman.expect.assertions.none
import network.marsys.smarthome.domain.entity.EntityIdentifier
import network.marsys.smarthome.domain.entity.Light
import network.marsys.smarthome.domain.entity.naming
import network.marsys.smarthome.hub.feature.entity.domain.event.EntityBecameUnavailable
import network.marsys.smarthome.hub.feature.entity.domain.event.EntityDiscovered
import network.marsys.smarthome.hub.feature.entity.domain.event.EntityProvisioned
import network.marsys.smarthome.hub.feature.entity.domain.event.Event

val InMemoryEventStoreTest by testSuite(
    name = "In-memory event store tests",
) {
    test(name = "An empty event store should return an empty list of entities") {
        val store = InMemoryEventStore()
        expectThat(store.invoke())
            .isEmpty()
    }

    test(name = "Loading an unknown entity identifier should return an empty list of events") {
        val store = InMemoryEventStore()
        expectThat(store.load(EntityIdentifier("some-identifier")))
            .isEmpty()
    }

    testSuite(
        name = "Events are stored in per identifier in append order",
    ) {
        test(name = "Appending events for a single entity identifier should return the events in append order on load") {
            val store = InMemoryEventStore()
            val identifier = EntityIdentifier("light.living-room")

            val events = listOf(
                EntityProvisioned(
                    identifier = identifier,
                    type = Light,
                    naming = Light.naming(),
                ),
                EntityBecameUnavailable(
                    identifier = identifier,
                ),
            )

            store.append(*events.toTypedArray())

            expectThat(store.load(identifier))
                .isEqualTo(events)
        }

        test(name = "Appending events for a multiple entity identifiers should return the events in append order on load") {
            val store = InMemoryEventStore()
            val identifier = EntityIdentifier("light.living-room")

            val events = listOf(
                EntityProvisioned(
                    identifier = identifier,
                    type = Light,
                    naming = Light.naming(),
                ),
                EntityProvisioned(
                    identifier = EntityIdentifier("light.kitchen"),
                    type = Light,
                    naming = Light.naming(),
                ),
                EntityBecameUnavailable(
                    identifier = identifier,
                ),
            )

            store.append(*events.toTypedArray())

            expectThat(store.load(identifier))
                .all {
                    get(Event::identifier)
                        .isEqualTo(identifier)
                }
        }
    }

    testSuite(
        name = "Finding entities by identifier",
    ) {
        test(name = "Finding an existing entity should return the entity") {
            val store = InMemoryEventStore()
            val identifier = EntityIdentifier("light.living-room")

            val events = listOf(
                EntityProvisioned(
                    identifier = identifier,
                    type = Light,
                    naming = Light.naming(),
                ),
            )

            store.append(*events.toTypedArray())

            expectThat(store.invoke(identifier))
                .isA<Light>()
        }

        test(name = "Finding an unknown entity should return null") {
            val store = InMemoryEventStore()
            val identifier = EntityIdentifier("light.living-room")

            expectThat(store.invoke(identifier))
                .isNull()
        }
    }
}
