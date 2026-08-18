package network.marsys.smarthome.hub.feature.integration.application

import de.infix.testBalloon.framework.core.testSuite
import dev.nmarsman.expect.api.expectThat
import dev.nmarsman.expect.assertions.contains
import dev.nmarsman.expect.assertions.count
import dev.nmarsman.expect.assertions.filter
import dev.nmarsman.expect.assertions.isA
import dev.nmarsman.expect.assertions.isEmpty
import dev.nmarsman.expect.assertions.isEqualTo
import network.marsys.smarthome.domain.identifiers.EntityIdentifier
import network.marsys.smarthome.hub.feature.entity.domain.capability.Capability.Companion.optional
import network.marsys.smarthome.hub.feature.entity.domain.capability.Capability.Companion.required
import network.marsys.smarthome.hub.feature.entity.domain.capability.OnOff
import network.marsys.smarthome.hub.feature.entity.domain.entity.Light
import network.marsys.smarthome.hub.feature.entity.domain.event.EntityDiscovered
import network.marsys.smarthome.hub.feature.entity.domain.event.EntityProvisioned
import network.marsys.smarthome.hub.feature.entity.domain.event.Event
import network.marsys.smarthome.hub.feature.integration.application.ports.outbound.EventStore
import kotlin.collections.getOrPut

val IntegrationEventProcessorTest by testSuite(
    name = "Integration event processor tests",
) {
    val identifier = EntityIdentifier("entity.test")

    val provisioned = EntityProvisioned(
        identifier = identifier,
        type = Light,
    )

    val discovered = EntityDiscovered(
        identifier = identifier,
        capabilities = Light.State.Known(
            onOff = required(OnOff(current = true)),
            brightness = optional(null),
        ),
    )

    test(name = "Processing an entity provisioned event is accepted if the entity has no provisioned event processed yet.") {
        val store = FakeEventStore()
        val processor = IntegrationEventProcessor(eventStore = store)

        val result = processor.process(provisioned)

        expectThat(result)
            .isA<IntegrationEventProcessor.ProcessingResult.Accepted>()

        expectThat(store.load(entity = identifier))
            .contains(provisioned)
    }

    test(name = "Processing an entity provisioned event is rejected if the entity already has a provisioned event processed.") {
        val store = FakeEventStore()
        val processor = IntegrationEventProcessor(eventStore = store)

        processor.process(provisioned)
        val result = processor.process(provisioned)

        expectThat(result)
            .isA<IntegrationEventProcessor.ProcessingResult.Rejected>()
            .get(IntegrationEventProcessor.ProcessingResult.Rejected::reason)
            .isA<IntegrationEventProcessor.RejectionReason.AlreadyProvisioned>()

        expectThat(store.load(entity = identifier))
            .filter { it == provisioned }
            .count()
            .isEqualTo(1)
    }

    test(name = "Processing an entity discovered event is accepted if the entity has already processed a provisioned event.") {
        val store = FakeEventStore()
        val processor = IntegrationEventProcessor(eventStore = store)

        processor.process(provisioned)
        val result = processor.process(discovered)

        expectThat(result)
            .isA<IntegrationEventProcessor.ProcessingResult.Accepted>()

        expectThat(store.load(entity = identifier))
            .contains(provisioned, discovered)
    }

    test(name = "Processing an entity discovered event is rejected if the entity has not processed a provisioned event yet.") {
        val store = FakeEventStore()
        val processor = IntegrationEventProcessor(eventStore = store)

        val result = processor.process(discovered)

        expectThat(result)
            .isA<IntegrationEventProcessor.ProcessingResult.Rejected>()
            .get(IntegrationEventProcessor.ProcessingResult.Rejected::reason)
            .isA<IntegrationEventProcessor.RejectionReason.NotProvisioned>()

        expectThat(store.load(entity = identifier))
            .isEmpty()
    }
}

class FakeEventStore : EventStore {
    private val events = mutableMapOf<EntityIdentifier, MutableList<Event>>()

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
