package network.marsys.smarthome.hub.feature.entity.application

import de.infix.testBalloon.framework.core.testSuite
import dev.nmarsman.expect.api.expectThat
import dev.nmarsman.expect.api.expectThrows
import dev.nmarsman.expect.assertions.hasMessage
import dev.nmarsman.expect.assertions.isA
import dev.nmarsman.expect.assertions.isEqualTo
import dev.nmarsman.expect.assertions.isNotNull
import dev.nmarsman.expect.assertions.isNull
import dev.nmarsman.expect.assertions.isSameInstanceAs
import network.marsys.smarthome.domain.identifiers.EntityIdentifier
import network.marsys.smarthome.domain.unit.celsius
import network.marsys.smarthome.domain.unit.gigabytes
import network.marsys.smarthome.domain.unit.minutes
import network.marsys.smarthome.domain.unit.percent
import network.marsys.smarthome.hub.feature.entity.domain.capability.Capability
import network.marsys.smarthome.hub.feature.entity.domain.capability.Capability.Companion.optional
import network.marsys.smarthome.hub.feature.entity.domain.capability.Capability.Companion.required
import network.marsys.smarthome.hub.feature.entity.domain.capability.Duration
import network.marsys.smarthome.hub.feature.entity.domain.capability.MeasuredDataSize
import network.marsys.smarthome.hub.feature.entity.domain.capability.MeasuredLoad
import network.marsys.smarthome.hub.feature.entity.domain.capability.MeasuredTemperature
import network.marsys.smarthome.hub.feature.entity.domain.capability.OnOff
import network.marsys.smarthome.hub.feature.entity.domain.entity.Entity
import network.marsys.smarthome.hub.feature.entity.domain.entity.Light
import network.marsys.smarthome.hub.feature.entity.domain.entity.System
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
            .isEqualTo(
                expected = Light(
                    identifier = identifier,
                    state = Light.State.Unknown(),
                ),
            )

        expectThat(aggregate.entity)
            .with(Entity::identifier) { isEqualTo(identifier) }
            .get(Entity::state)
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

            val state = Light.State.Known(
                onOff = required(OnOff(current = true)),
                brightness = Capability.Unsupported,
            )

            val history = listOf(
                EntityDiscovered(
                    identifier = identifier,
                    state = state,
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

        val state = Light.State.Known(
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
                state = state,
            ),
        )

        val aggregate = EntityAggregate(history)

        expectThat(aggregate.entity)
            .isEqualTo(
                expected = Light(
                    identifier = identifier,
                    state = state,
                ),
            )
    }

    test(name = "Creating aggregate succeeds when provisioned and discovered entity events are supplied - system") {
        val identifier = EntityIdentifier("system.smarthome")

        val state = System.State.Known(
            info = hostInfo,
            processor = System.Processor(
                load = required(MeasuredLoad(current = 0.5.percent)),
                temperature = optional(MeasuredTemperature(current = 45.celsius)),
            ),
            memory = System.Memory(
                total = required(MeasuredDataSize(current = 8.gigabytes)),
                available = required(MeasuredDataSize(current = 4.gigabytes)),
                swap = System.Memory.Swap(
                    total = required(MeasuredDataSize(current = 2.gigabytes)),
                    used = required(MeasuredDataSize(current = 1.gigabytes)),
                ),
            ),
            uptime = System.Uptime(
                host = required(Duration(current = 90.minutes)),
                application = required(Duration(current = 77.minutes)),
            ),
        )

        val history = listOf(
            EntityProvisioned(
                identifier = identifier,
                type = System,
            ),
            EntityDiscovered(
                identifier = identifier,
                state = state,
            ),
        )

        val aggregate = EntityAggregate(history)

        expectThat(aggregate.entity)
            .isEqualTo(
                expected = System(
                    identifier = identifier,
                    state = state,
                ),
            )
    }

    test(name = "Creating aggregate fails when provisioned and discovered entity have different identifiers") {
        val identifier = EntityIdentifier("light.living-room")

        val state = Light.State.Known(
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
                state = state,
            ),
        )

        expectThrows<IllegalStateException> {
            EntityAggregate(history)
        }.hasMessage("Mismatched entity identifier 'light.kitchen' supplied while 'light.living-room' is expected.")
    }

    test(name = "Creating aggregate fails when discovered entity has unsupported state") {
        val identifier = EntityIdentifier("light.living-room")

        val state: Entity.State = System.State.Known(
            info = hostInfo,
            processor = System.Processor(
                load = required(MeasuredLoad(current = 0.5.percent)),
                temperature = optional(MeasuredTemperature(current = 45.celsius)),
            ),
            memory = System.Memory(
                total = required(MeasuredDataSize(current = 8.gigabytes)),
                available = required(MeasuredDataSize(current = 4.gigabytes)),
                swap = System.Memory.Swap(
                    total = required(MeasuredDataSize(current = 2.gigabytes)),
                    used = required(MeasuredDataSize(current = 1.gigabytes)),
                ),
            ),
            uptime = System.Uptime(
                host = required(Duration(current = 90.minutes)),
                application = required(Duration(current = 77.minutes)),
            ),
        )

        val history = listOf(
            EntityProvisioned(
                identifier = identifier,
                type = Light,
            ),
            EntityDiscovered(
                identifier = identifier,
                state = state,
            ),
        )

        expectThrows<IllegalStateException> {
            EntityAggregate(history)
        }.hasMessage("Entity state of type 'System.State.Known' supplied while 'Light.State.Known' is expected.")
    }

    test(name = "Initializing aggregate with entity became unavailable event after discovery sets state to unknown") {
        val identifier = EntityIdentifier("light.living-room")

        val state = Light.State.Known(
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
                state = state,
            ),
            EntityBecameUnavailable(
                identifier = identifier,
            ),
        )

        val aggregate = EntityAggregate(history)

        expectThat(aggregate.entity.state)
            .isA<Light.State.Unknown>()
            .get(Light.State.Unknown::lastKnown)
            .isSameInstanceAs(state)
    }

    test(name = "Initializing aggregate with multiple entity became unavailable events after discovery keeps the last known state") {
        val identifier = EntityIdentifier("light.living-room")

        val state = Light.State.Known(
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
                state = state,
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
            .isSameInstanceAs(state)
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

private val hostInfo = System.HostInfo(
    device = System.HostInfo.Device(
        manufacturer = "Test Manufacturer",
        model = "Test Model",
        architecture = "x86_64",
        physicalCores = 4,
        logicalCores = 8,
    ),
    operatingSystem = System.HostInfo.OperatingSystem(
        description = "Test OS",
        family = "Test Family",
        version = "1.0.0",
        bitness = 64,
    ),
)
