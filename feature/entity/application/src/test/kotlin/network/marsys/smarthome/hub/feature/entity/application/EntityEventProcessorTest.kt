package network.marsys.smarthome.hub.feature.entity.application

import de.infix.testBalloon.framework.core.testSuite
import dev.nmarsman.expect.api.expectThat
import dev.nmarsman.expect.assertions.contains
import dev.nmarsman.expect.assertions.count
import dev.nmarsman.expect.assertions.filter
import dev.nmarsman.expect.assertions.isA
import dev.nmarsman.expect.assertions.isEmpty
import dev.nmarsman.expect.assertions.isEqualTo
import network.marsys.smarthome.domain.identifiers.EntityIdentifier
import network.marsys.smarthome.domain.unit.percent
import network.marsys.smarthome.hub.feature.entity.application.internal.EntityEventProcessor
import network.marsys.smarthome.hub.feature.entity.application.ports.inbound.ProcessEntityEvent
import network.marsys.smarthome.hub.feature.entity.application.ports.outbound.EntityEventPublisher
import network.marsys.smarthome.hub.feature.entity.application.ports.outbound.EventStore
import network.marsys.smarthome.hub.feature.entity.domain.capability.Brightness
import network.marsys.smarthome.hub.feature.entity.domain.capability.Capability.Companion.optional
import network.marsys.smarthome.hub.feature.entity.domain.capability.Capability.Companion.required
import network.marsys.smarthome.hub.feature.entity.domain.capability.MeasuredLoad
import network.marsys.smarthome.hub.feature.entity.domain.capability.OnOff
import network.marsys.smarthome.hub.feature.entity.domain.entity.Light
import network.marsys.smarthome.hub.feature.entity.domain.event.CapabilityUpdated
import network.marsys.smarthome.hub.feature.entity.domain.event.EntityBecameUnavailable
import network.marsys.smarthome.hub.feature.entity.domain.event.EntityDiscovered
import network.marsys.smarthome.hub.feature.entity.domain.event.EntityProvisioned
import network.marsys.smarthome.hub.feature.entity.domain.event.Event
import kotlin.collections.getOrPut

val EntityEventProcessorTest by testSuite(
    name = "Entity event processor tests",
) {
    val identifier = EntityIdentifier("entity.test")

    val provisioned = EntityProvisioned(
        identifier = identifier,
        type = Light,
    )

    val discovered = EntityDiscovered(
        identifier = identifier,
        state = Light.State.Known(
            onOff = required(OnOff(current = true)),
            brightness = optional(null),
        ),
    )

    val becameUnavailable = EntityBecameUnavailable(
        identifier = identifier,
    )

    val capabilityUpdated = CapabilityUpdated(
        identifier = identifier,
        capability = OnOff(current = false),
    )

    test(name = "Processing an entity provisioned event is accepted if the entity has no provisioned event processed yet.") {
        val store = FakeEventStore()

        val processor = EntityEventProcessor(
            store = store,
            publisher = {},
        )

        val result = processor.invoke(provisioned)

        expectThat(result)
            .isA<ProcessEntityEvent.Result.Accepted>()

        expectThat(store.load(entity = identifier))
            .contains(provisioned)
    }

    test(name = "Processing an entity provisioned event is accepted if the entity has no provisioned event processed yet.") {
        val store = FakeEventStore()
        val publisher = FakeEventPublisher()

        val processor = EntityEventProcessor(
            store = store,
            publisher = publisher,
        )

        val result = processor.invoke(provisioned)

        expectThat(result)
            .isA<ProcessEntityEvent.Result.Accepted>()

        expectThat(publisher.published)
            .contains(provisioned)
    }

    test(name = "Processing an entity provisioned event is rejected if the entity already has a provisioned event processed.") {
        val store = FakeEventStore()

        val processor = EntityEventProcessor(
            store = store,
            publisher = {},
        )

        processor.invoke(provisioned)
        val result = processor.invoke(provisioned)

        expectThat(result)
            .isA<ProcessEntityEvent.Result.Rejected>()
            .get(ProcessEntityEvent.Result.Rejected::reason)
            .isA<ProcessEntityEvent.Result.Rejected.Reason.AlreadyProvisioned>()

        expectThat(store.load(entity = identifier))
            .filter { it == provisioned }
            .count()
            .isEqualTo(1)
    }

    test(name = "Processing an entity discovered event is accepted if the entity has already processed a provisioned event.") {
        val store = FakeEventStore(
            history = listOf(provisioned),
        )

        val processor = EntityEventProcessor(
            store = store,
            publisher = {},
        )

        val result = processor.invoke(discovered)

        expectThat(result)
            .isA<ProcessEntityEvent.Result.Accepted>()

        expectThat(store.load(entity = identifier))
            .contains(provisioned, discovered)
    }

    test(name = "Processing an entity discovered event is rejected if the entity has not processed a provisioned event yet.") {
        val store = FakeEventStore()

        val processor = EntityEventProcessor(
            store = store,
            publisher = {},
        )

        val result = processor.invoke(discovered)

        expectThat(result)
            .isA<ProcessEntityEvent.Result.Rejected>()
            .get(ProcessEntityEvent.Result.Rejected::reason)
            .isA<ProcessEntityEvent.Result.Rejected.Reason.NotProvisioned>()

        expectThat(store.load(entity = identifier))
            .isEmpty()
    }

    test("Processing an entity became unavailable event is accepted if the entity has a known state.") {
        val store = FakeEventStore(
            history = listOf(provisioned, discovered),
        )

        val processor = EntityEventProcessor(
            store = store,
            publisher = {},
        )

        val result = processor.invoke(becameUnavailable)

        expectThat(result)
            .isA<ProcessEntityEvent.Result.Accepted>()

        expectThat(store.load(entity = identifier))
            .contains(provisioned, discovered, becameUnavailable)
    }

    test("Processing an entity became unavailable event is ignored if the entity is not discovered yet.") {
        val store = FakeEventStore(
            history = listOf(provisioned),
        )

        val processor = EntityEventProcessor(
            store = store,
            publisher = {},
        )

        val result = processor.invoke(becameUnavailable)

        expectThat(result)
            .isA<ProcessEntityEvent.Result.Ignored>()

        expectThat(store.load(entity = identifier))
            .contains(provisioned)
    }

    test("Processing an entity became unavailable event is ignored if the entity already has an unknown state.") {
        val store = FakeEventStore(
            history = listOf(provisioned, discovered, becameUnavailable),
        )

        val processor = EntityEventProcessor(
            store = store,
            publisher = {},
        )

        val result = processor.invoke(becameUnavailable)

        expectThat(result)
            .isA<ProcessEntityEvent.Result.Ignored>()

        expectThat(store.load(entity = identifier))
            .contains(provisioned, discovered, becameUnavailable)
    }

    test("Processing a capability changed event is accepted if the entity has a known state and the state is actually different.") {
        val store = FakeEventStore(
            history = listOf(provisioned, discovered),
        )

        val processor = EntityEventProcessor(
            store = store,
            publisher = {},
        )

        val result = processor.invoke(capabilityUpdated)

        expectThat(result)
            .isA<ProcessEntityEvent.Result.Accepted>()

        expectThat(store.load(entity = identifier))
            .contains(provisioned, discovered, capabilityUpdated)
    }

    test("Processing a capability changed event is ignored if the entity has a known state and the state is the same.") {
        val store = FakeEventStore(
            history = listOf(provisioned, discovered),
        )

        val processor = EntityEventProcessor(
            store = store,
            publisher = {},
        )

        val result = processor.invoke(
            event = capabilityUpdated.copy(
                capability = OnOff(current = true),
            ),
        )

        expectThat(result)
            .isA<ProcessEntityEvent.Result.Ignored>()

        expectThat(store.load(entity = identifier))
            .contains(provisioned, discovered)
    }

    test("Processing a capability changed event is ignored if the entity has a known state and the state was unsupported") {
        val capabilityUpdated = CapabilityUpdated(
            identifier = identifier,
            capability = Brightness(current = 50.percent),
        )

        val store = FakeEventStore(
            history = listOf(provisioned, discovered),
        )

        val processor = EntityEventProcessor(
            store = store,
            publisher = {},
        )

        val result = processor.invoke(capabilityUpdated)

        expectThat(result)
            .isA<ProcessEntityEvent.Result.Rejected>()
            .get(ProcessEntityEvent.Result.Rejected::reason)
            .isA<ProcessEntityEvent.Result.Rejected.Reason.CapabilityNotPresent>()

        expectThat(store.load(entity = identifier))
            .contains(provisioned, discovered)
    }

    test("Processing a capability changed event is ignored if the entity has a known state but an unknown capability passed") {
        val capabilityUpdated = CapabilityUpdated(
            identifier = identifier,
            capability = MeasuredLoad(current = 50.percent),
        )

        val store = FakeEventStore(
            history = listOf(provisioned, discovered),
        )

        val processor = EntityEventProcessor(
            store = store,
            publisher = {},
        )

        val result = processor.invoke(capabilityUpdated)

        expectThat(result)
            .isA<ProcessEntityEvent.Result.Rejected>()
            .get(ProcessEntityEvent.Result.Rejected::reason)
            .isA<ProcessEntityEvent.Result.Rejected.Reason.CapabilityNotPresent>()

        expectThat(store.load(entity = identifier))
            .contains(provisioned, discovered)
    }

    test("Processing a capability changed event is ignored if the entity has an unknown state.") {
        val store = FakeEventStore(
            history = listOf(provisioned),
        )

        val processor = EntityEventProcessor(
            store = store,
            publisher = {},
        )

        val result = processor.invoke(capabilityUpdated)

        expectThat(result)
            .isA<ProcessEntityEvent.Result.Rejected>()
            .get(ProcessEntityEvent.Result.Rejected::reason)
            .isA<ProcessEntityEvent.Result.Rejected.Reason.NotDiscovered>()

        expectThat(store.load(entity = identifier))
            .contains(provisioned)
    }

    test("Processing an entity with events out of order saved ignores new events.") {
        val store = FakeEventStore(
            history = listOf(discovered, provisioned),
        )

        val processor = EntityEventProcessor(
            store = store,
            publisher = {},
        )

        val result = processor.invoke(becameUnavailable)

        expectThat(result)
            .isA<ProcessEntityEvent.Result.Ignored>()

        expectThat(store.load(entity = identifier))
            .contains(discovered, provisioned)
    }
}

class FakeEventStore(
    history: Collection<Event> = emptyList(),
) : EventStore {
    private val events: MutableMap<EntityIdentifier, MutableList<Event>> = history
        .groupBy { it.identifier }
        .mapValues { (_, events) -> events.toMutableList() }
        .toMutableMap()

    override suspend fun append(vararg events: Event) {
        events.forEach { event ->
            append(event = event)
        }
    }

    private fun append(event: Event) {
        events.getOrPut(key = event.identifier, defaultValue = ::mutableListOf)
            .add(event)
    }

    override suspend fun load(entity: EntityIdentifier): Collection<Event> =
        events.getOrDefault(entity, emptyList()).toList()
}

class FakeEventPublisher : EntityEventPublisher {
    private val events: MutableList<Event> = mutableListOf()
    val published: List<Event> get() = events.toList()

    override suspend fun publish(event: Event) {
        events.add(event)
    }
}
