package network.marsys.smarthome.hub.feature.entity.application

import de.infix.testBalloon.framework.core.testSuite
import dev.nmarsman.expect.api.expectThat
import dev.nmarsman.expect.api.expectThrows
import dev.nmarsman.expect.assertions.hasMessage
import dev.nmarsman.expect.assertions.hasMessageEndingWith
import dev.nmarsman.expect.assertions.hasMessageStartingWith
import dev.nmarsman.expect.assertions.isA
import dev.nmarsman.expect.assertions.isEqualTo
import dev.nmarsman.expect.assertions.isNotNull
import dev.nmarsman.expect.assertions.isNull
import dev.nmarsman.expect.assertions.isSameInstanceAs
import network.marsys.smarthome.domain.identifiers.EntityIdentifier
import network.marsys.smarthome.hub.feature.entity.domain.capability.Capability
import network.marsys.smarthome.hub.feature.entity.domain.capability.Capability.Companion.required
import network.marsys.smarthome.hub.feature.entity.domain.capability.OnOff
import network.marsys.smarthome.hub.feature.entity.domain.entity.Entity
import network.marsys.smarthome.hub.feature.entity.domain.entity.Light
import network.marsys.smarthome.hub.feature.entity.domain.event.EntityBecameUnavailable
import network.marsys.smarthome.hub.feature.entity.domain.event.EntityDiscovered
import network.marsys.smarthome.hub.feature.entity.domain.event.EntityProvisioned

val AggregateTest by testSuite(
    name = "Aggregate tests",
) {
    test(name = "Creating aggregate succeeds when entity provisioned event is provided") {
        val identifier = EntityIdentifier("light.living-room")

        val history = listOf(
            EntityProvisioned(
                identifier = identifier,
                type = Light,
            ),
        )

        val aggregate = EntityAggregate(history)

        expectThat(aggregate.entity)
            .isEqualTo(Light.createWithUnknown(identifier))

        expectThat(aggregate.entity)
            .with(Entity<*, *>::identifier) { isEqualTo(identifier) }
            .get(Entity<*, *>::state)
            .isA<Light.State.Unknown>()
    }

    test(name = "Creating aggregate fails when no events are provided") {
        expectThrows<IllegalStateException> {
            EntityAggregate(emptyList())
        }.hasMessage("Entity event history must start with entity provisioned event.")
    }

    test(name = "Creating aggregate fails when multiple provisioning events are provided") {
        expectThrows<IllegalStateException> {
            val identifier = EntityIdentifier("light.living-room")

            val history = listOf(
                EntityProvisioned(
                    identifier = identifier,
                    type = Light,
                ),
                EntityProvisioned(
                    identifier = identifier,
                    type = Light,
                ),
            )

            EntityAggregate(history)
        }.hasMessage("Entity has already been provisioned.")
    }

    test(name = "Creating aggregate fails when entity provisioned event is not the first event provided") {
        expectThrows<IllegalStateException> {
            val identifier = EntityIdentifier("light.living-room")

            val capabilities: Light.Capabilities = Light.State.Known(
                onOff = required(OnOff(current = true)),
                brightness = Capability.Unsupported,
            )

            val history = listOf(
                EntityDiscovered(
                    identifier = identifier,
                    capabilities = capabilities,
                ),
                EntityProvisioned(
                    identifier = identifier,
                    type = Light,
                ),
            )

            EntityAggregate(history)
        }.hasMessage("Entity event history must start with entity provisioned event.")
    }

    test(name = "Creating aggregate succeeds when provisioned and discovered entity events are supplied") {
        val identifier = EntityIdentifier("light.living-room")

        val capabilities: Light.Capabilities = Light.State.Known(
            onOff = required(OnOff(current = true)),
            brightness = Capability.Unsupported,
        )

        val history = listOf(
            EntityProvisioned(
                identifier = identifier,
                type = Light,
            ),
            EntityDiscovered(
                identifier = identifier,
                capabilities = capabilities,
            ),
        )

        val aggregate = EntityAggregate(history)

        expectThat(aggregate.entity)
            .isEqualTo(Light.createWithKnown(identifier, capabilities))
    }

    test(name = "Creating aggregate fails when provisioned and discovered entity have different identifiers") {
        val identifier = EntityIdentifier("light.living-room")

        val capabilities: Light.Capabilities = Light.State.Known(
            onOff = required(OnOff(current = true)),
            brightness = Capability.Unsupported,
        )

        val history = listOf(
            EntityProvisioned(
                identifier = identifier,
                type = Light,
            ),
            EntityDiscovered(
                identifier = EntityIdentifier("light.kitchen"),
                capabilities = capabilities,
            ),
        )

        expectThrows<IllegalStateException> {
            EntityAggregate(history)
        }.hasMessage("Mismatched entity identifier 'light.kitchen' supplied while 'light.living-room' is expected.")
    }

    test(name = "Creating aggregate fails when discovered entity has unsupported capabilities") {
        val identifier = EntityIdentifier("light.living-room")

        val capabilities: Entity.Capabilities = object : Entity.Capabilities {}

        val history = listOf(
            EntityProvisioned(
                identifier = identifier,
                type = Light,
            ),
            EntityDiscovered(
                identifier = identifier,
                capabilities = capabilities,
            ),
        )

        expectThrows<IllegalStateException> {
            EntityAggregate(history)
        }.hasMessage("Entity capabilities of type 'null' supplied while 'Light.State.Known' is expected.")
    }

    test(name = "Initializing aggregate with entity became unavailable event after discovery sets state to unknown") {
        val identifier = EntityIdentifier("light.living-room")

        val capabilities: Light.Capabilities = Light.State.Known(
            onOff = required(OnOff(current = true)),
            brightness = Capability.Unsupported,
        )

        val history = listOf(
            EntityProvisioned(
                identifier = identifier,
                type = Light,
            ),
            EntityDiscovered(
                identifier = identifier,
                capabilities = capabilities,
            ),
            EntityBecameUnavailable(
                identifier = identifier,
            ),
        )

        val aggregate = EntityAggregate(history)

        expectThat(aggregate.entity.state)
            .isA<Light.State.Unknown>()
            .get(Light.State.Unknown::lastKnown)
            .isSameInstanceAs(capabilities)
    }

    test(name = "Initializing aggregate with multiple entity became unavailable events after discovery keeps the last known state") {
        val identifier = EntityIdentifier("light.living-room")

        val capabilities: Light.Capabilities = Light.State.Known(
            onOff = required(OnOff(current = true)),
            brightness = Capability.Unsupported,
        )

        val history = listOf(
            EntityProvisioned(
                identifier = identifier,
                type = Light,
            ),
            EntityDiscovered(
                identifier = identifier,
                capabilities = capabilities,
            ),
            EntityBecameUnavailable(
                identifier = identifier,
            ),
            EntityBecameUnavailable(
                identifier = identifier,
            ),
        )

        val aggregate = EntityAggregate(history)

        expectThat(aggregate.entity.state)
            .isA<Light.State.Unknown>()
            .get(Light.State.Unknown::lastKnown)
            .isNotNull()
            .isSameInstanceAs(capabilities)
    }

    test(name = "Initializing aggregate with entity became unavailable event without discovery sets state to unknown") {
        val identifier = EntityIdentifier("light.living-room")

        val history = listOf(
            EntityProvisioned(
                identifier = identifier,
                type = Light,
            ),
            EntityBecameUnavailable(
                identifier = identifier,
            ),
        )

        val aggregate = EntityAggregate(history)

        expectThat(aggregate.entity.state)
            .isA<Light.State.Unknown>()
            .get(Light.State.Unknown::lastKnown)
            .isNull()
    }

    test(name = "Initializing aggregate with multiple entity became unavailable events without discovery sets state to unknown") {
        val identifier = EntityIdentifier("light.living-room")

        val history = listOf(
            EntityProvisioned(
                identifier = identifier,
                type = Light,
            ),
            EntityBecameUnavailable(
                identifier = identifier,
            ),
            EntityBecameUnavailable(
                identifier = identifier,
            ),
        )

        val aggregate = EntityAggregate(history)

        expectThat(aggregate.entity.state)
            .isA<Light.State.Unknown>()
            .get(Light.State.Unknown::lastKnown)
            .isNull()
    }
}
