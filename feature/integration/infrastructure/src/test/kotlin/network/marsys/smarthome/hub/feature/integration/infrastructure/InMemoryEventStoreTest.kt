package network.marsys.smarthome.hub.feature.integration.infrastructure

import de.infix.testBalloon.framework.core.testSuite
import dev.nmarsman.expect.api.expectThat
import dev.nmarsman.expect.assertions.containsExactly
import dev.nmarsman.expect.assertions.isA
import dev.nmarsman.expect.assertions.isEmpty
import network.marsys.smarthome.domain.identifiers.EntityIdentifier
import network.marsys.smarthome.hub.feature.entity.domain.entity.Light
import network.marsys.smarthome.hub.feature.entity.domain.event.EntityProvisioned
import network.marsys.smarthome.hub.feature.entity.domain.event.Event

val InMemoryEventStoreTest by testSuite(
    name = "In-memory event store tests",
) {
    val identifier = EntityIdentifier("entity.test")

    test(name = "Loading events for an unknown entity returns an empty list") {
        val store = InMemoryEventStore()

        expectThat(store.load(identifier))
            .isEmpty()
    }

    test(name = "Loading events for an entity returns only it's events") {
        val store = InMemoryEventStore()

        val provisioned = EntityProvisioned(
            identifier = identifier,
            type = Light,
        )

        store.append(provisioned)

        expectThat(store.load(EntityIdentifier("entity.other")))
            .isEmpty()
    }

    test(name = "Appending events for an unknown entity succeeds") {
        val store = InMemoryEventStore()

        val provisioned = EntityProvisioned(
            identifier = identifier,
            type = Light,
        )

        store.append(provisioned)

        expectThat(store.load(identifier))
            .isA<List<Event>>()
            .containsExactly(provisioned)
    }

    test(name = "Appending multiple events for an unknown entity succeeds") {
        val store = InMemoryEventStore()

        val provisioned = EntityProvisioned(
            identifier = identifier,
            type = Light,
        )

        store.append(provisioned, provisioned, provisioned)

        expectThat(store.load(identifier))
            .isA<List<Event>>()
            .containsExactly(provisioned, provisioned, provisioned)
    }
}
