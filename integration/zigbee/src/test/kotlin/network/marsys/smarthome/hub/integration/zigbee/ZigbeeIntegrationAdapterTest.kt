package network.marsys.smarthome.hub.integration.zigbee

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import de.infix.testBalloon.framework.core.testSuite
import dev.nmarsman.expect.api.expectThat
import dev.nmarsman.expect.assertions.contains
import dev.nmarsman.expect.assertions.containsExactly
import dev.nmarsman.expect.assertions.isA
import dev.nmarsman.expect.assertions.isEqualTo
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import network.marsys.smarthome.domain.TranslationKey
import network.marsys.smarthome.domain.capability.Brightness
import network.marsys.smarthome.domain.capability.Capability
import network.marsys.smarthome.domain.capability.Capability.Companion.optional
import network.marsys.smarthome.domain.capability.Capability.Companion.required
import network.marsys.smarthome.domain.capability.OnOff
import network.marsys.smarthome.domain.entity.Entity
import network.marsys.smarthome.domain.entity.EntityIdentifier
import network.marsys.smarthome.domain.entity.EntityNaming
import network.marsys.smarthome.domain.entity.Light
import network.marsys.smarthome.domain.entity.System
import network.marsys.smarthome.domain.entity.naming
import network.marsys.smarthome.domain.identifiers.IntegrationIdentifier
import network.marsys.smarthome.domain.unit.percent
import network.marsys.smarthome.hub.feature.entity.domain.event.EntityDiscovered
import network.marsys.smarthome.hub.feature.entity.domain.event.EntityProvisioned
import network.marsys.smarthome.hub.feature.entity.domain.event.Event
import network.marsys.smarthome.hub.feature.integration.application.ports.outbound.EntityEventPublisher
import network.marsys.smarthome.hub.feature.integration.application.ports.outbound.EntityQuery
import network.marsys.smarthome.hub.integration.zigbee.helper.FakeMqttClient
import network.marsys.smarthome.hub.integration.zigbee.helper.entity
import kotlin.time.Clock
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
val ZigbeeIntegrationAdapterTest by testSuite(
    name = "Zigbee integration adapter tests",
) {
    test(name = "Receiving a light device should provision a Light entity") {
        runTest {
            withZigbeeIntegrationAdapter { client, _ ->
                client.receive(light())

                val event = awaitItem()

                expectThat(event)
                    .isEqualTo(
                        EntityProvisioned(
                            identifier = EntityIdentifier("zigbee.0x943469fffeb3c843"),
                            type = Light,
                            naming = EntityNaming(
                                key = TranslationKey("light.kitchen"),
                            ),
                            occurredAt = instant,
                        ),
                    )

                runCurrent()
                expectNoEvents()
            }
        }
    }

    test(name = "Receiving multiple light devices should provision multiple Light entities") {
        runTest {
            withZigbeeIntegrationAdapter { client, _ ->
                client.receive(
                    devices = arrayOf(
                        light(
                            ieeeAddress = "0x001",
                            friendlyName = "light.kitchen",
                        ),
                        light(
                            ieeeAddress = "0x002",
                            friendlyName = "light.living-room",
                        ),
                    ),
                )

                val events = listOf(awaitItem(), awaitItem())

                expectThat(events)
                    .contains(
                        listOf(
                            EntityProvisioned(
                                identifier = EntityIdentifier("zigbee.0x001"),
                                type = Light,
                                naming = EntityNaming(
                                    key = TranslationKey("light.kitchen"),
                                ),
                                occurredAt = instant,
                            ),
                            EntityProvisioned(
                                identifier = EntityIdentifier("zigbee.0x002"),
                                type = Light,
                                naming = EntityNaming(
                                    key = TranslationKey("light.living-room"),
                                ),
                                occurredAt = instant,
                            ),
                        ),
                    )

                runCurrent()
                expectNoEvents()
            }
        }
    }

    test(name = "Receiving different device types should only provision recognized entities") {
        runTest {
            withZigbeeIntegrationAdapter { client, _ ->
                client.receive(
                    devices = arrayOf(
                        light(),
                        thermostat(),
                    ),
                )

                expectThat(awaitItem())
                    .isEqualTo(
                        EntityProvisioned(
                            identifier = EntityIdentifier("zigbee.0x943469fffeb3c843"),
                            type = Light,
                            naming = EntityNaming(
                                key = TranslationKey("light.kitchen"),
                            ),
                            occurredAt = instant,
                        ),
                    )

                runCurrent()
                expectNoEvents()
            }
        }
    }

    test(name = "Receiving an unsupported device should not provision an entity") {
        runTest {
            withZigbeeIntegrationAdapter { client, _ ->
                client.receive(
                    light(supported = false),
                )

                runCurrent()
                expectNoEvents()
            }
        }
    }

    test(name = "Receiving a disabled device should not provision an entity") {
        runTest {
            withZigbeeIntegrationAdapter { client, _ ->
                client.receive(
                    light(disabled = true),
                )

                runCurrent()
                expectNoEvents()
            }
        }
    }

    test(name = "Receiving an unrecognized device should not provision an entity") {
        runTest {
            withZigbeeIntegrationAdapter { client, _ ->
                client.receive(thermostat())

                runCurrent()
                expectNoEvents()
            }
        }
    }

    test(name = "Receiving an empty device snapshot should not provision entities") {
        runTest {
            withZigbeeIntegrationAdapter { client, _ ->
                client.receive()

                runCurrent()
                expectNoEvents()
            }
        }
    }

    test(name = "Receiving a light without a semantic friendly name should use default naming") {
        runTest {
            withZigbeeIntegrationAdapter { client, _ ->
                client.receive(
                    light(
                        friendlyName = "0x943469fffeb3c843",
                    ),
                )

                expectThat(awaitItem()).isEqualTo(
                    EntityProvisioned(
                        identifier = EntityIdentifier("zigbee.0x943469fffeb3c843"),
                        type = Light,
                        naming = Light.naming(),
                        occurredAt = instant,
                    ),
                )
            }
        }
    }

    test(name = "Receiving a light with an unrelated friendly name should use default naming") {
        runTest {
            withZigbeeIntegrationAdapter { client, _ ->
                client.receive(
                    light(
                        friendlyName = "dimmer_123",
                    ),
                )

                expectThat(awaitItem()).isEqualTo(
                    EntityProvisioned(
                        identifier = EntityIdentifier("zigbee.0x943469fffeb3c843"),
                        type = Light,
                        naming = Light.naming(),
                        occurredAt = instant,
                    ),
                )
            }
        }
    }

    test(name = "Renaming an unprovisioned device should not change the entity identifier") {
        runTest {
            withZigbeeIntegrationAdapter { client, _ ->
                client.receive(
                    light(
                        friendlyName = "light.kitchen",
                    ),
                )

                val original = awaitItem() as EntityProvisioned

                client.receive(
                    light(
                        friendlyName = "light.kitchen-ceiling",
                    ),
                )

                val renamed = awaitItem() as EntityProvisioned

                expectThat(renamed.identifier)
                    .isEqualTo(original.identifier)

                expectThat(renamed.naming)
                    .isEqualTo(
                        EntityNaming(
                            key = TranslationKey("light.kitchen-ceiling"),
                        ),
                    )
            }
        }
    }

    test(name = "Devices with identical friendly names should have different entity identifiers") {
        runTest {
            withZigbeeIntegrationAdapter { client, _ ->
                client.receive(
                    devices = arrayOf(
                        light(
                            ieeeAddress = "0x001",
                            friendlyName = "light.kitchen",
                        ),
                        light(
                            ieeeAddress = "0x002",
                            friendlyName = "light.kitchen",
                        ),
                    ),
                )

                val first = awaitItem() as EntityProvisioned
                val second = awaitItem() as EntityProvisioned

                expectThat(first.identifier)
                    .isEqualTo(EntityIdentifier("zigbee.0x001"))

                expectThat(second.identifier)
                    .isEqualTo(EntityIdentifier("zigbee.0x002"))
            }
        }
    }

    test(name = "Receiving an identical snapshot before successful provisioning should propose provisioning again") {
        runTest {
            withZigbeeIntegrationAdapter { client, _ ->
                val device = light()

                client.receive(device)
                val first = awaitItem()

                client.receive(device)
                val second = awaitItem()

                expectThat(second)
                    .isEqualTo(first)

                runCurrent()
                expectNoEvents()
            }
        }
    }

    test(name = "Receiving device information on an unrelated topic should be ignored") {
        runTest {
            withZigbeeIntegrationAdapter { client, _ ->
                client.receive(
                    topic = "zigbee2mqtt/bridge/other",
                    payload = "[${light()}]",
                )

                runCurrent()
                expectNoEvents()

                client.receive(light())

                expectThat(awaitItem())
                    .isA<EntityProvisioned>()
            }
        }
    }

    test(name = "Receiving invalid device information should not stop message processing") {
        runTest {
            withZigbeeIntegrationAdapter { client, _ ->
                client.receive(
                    topic = "zigbee2mqtt/bridge/devices",
                    payload = "invalid-json",
                )

                runCurrent()
                expectNoEvents()

                client.receive(light())

                expectThat(awaitItem())
                    .isA<EntityProvisioned>()
            }
        }
    }

    test(name = "Restarting the integration should resubscribe to Zigbee2MQTT") {
        runTest {
            withZigbeeIntegrationAdapter { client, adapter ->
                adapter.restart()

                expectThat(client.subscription.await())
                    .containsExactly("zigbee2mqtt/#")

                client.receive(light())

                expectThat(awaitItem())
                    .isEqualTo(
                        EntityProvisioned(
                            identifier = EntityIdentifier("zigbee.0x943469fffeb3c843"),
                            type = Light,
                            naming = EntityNaming(
                                key = TranslationKey("light.kitchen"),
                            ),
                            occurredAt = instant,
                        ),
                    )

                runCurrent()
                expectNoEvents()
            }
        }
    }

    test(name = "Receiving an already provisioned device should not provision an entity") {
        runTest {
            val query = FakeEntityQuery().apply {
                register(entity = entity())
            }

            withZigbeeIntegrationAdapter(query = query) { client, _ ->
                client.receive(devices = arrayOf(light()))

                runCurrent()
                expectNoEvents()
            }
        }
    }

    test(name = "Receiving an already discovered device should not provision an entity") {
        runTest {
            val query = FakeEntityQuery().apply {
                register(
                    entity = entity(
                        state = Light.State.Known(
                            onOff = required(OnOff(current = true)),
                            brightness = optional(Brightness(current = 50.0.percent)),
                        ),
                    ),
                )
            }

            withZigbeeIntegrationAdapter(query = query) { client, _ ->
                client.receive(light())

                runCurrent()
                expectNoEvents()
            }
        }
    }

    test(name = "Receiving provisioned and unprovisioned devices should only provision unknown entities") {
        runTest {
            val query = FakeEntityQuery().apply {
                register(entity = entity(ieeeAddress = "0x001"))
            }

            withZigbeeIntegrationAdapter(query = query) { client, _ ->
                client.receive(
                    devices = arrayOf(
                        light(
                            ieeeAddress = "0x001",
                            friendlyName = "light.kitchen",
                        ),
                        light(
                            ieeeAddress = "0x002",
                            friendlyName = "light.living-room",
                        ),
                    ),
                )

                expectThat(awaitItem())
                    .isEqualTo(
                        EntityProvisioned(
                            identifier = EntityIdentifier("zigbee.0x002"),
                            type = Light,
                            naming = EntityNaming(
                                key = TranslationKey("light.living-room"),
                            ),
                            occurredAt = instant,
                        ),
                    )

                runCurrent()
                expectNoEvents()
            }
        }
    }

    test(name = "Receiving an identical snapshot after successful provisioning should not propose provisioning again") {
        runTest {
            val query = FakeEntityQuery()

            withZigbeeIntegrationAdapter(query = query) { client, _ ->
                val device = light()

                client.receive(device)

                expectThat(awaitItem())
                    .isEqualTo(
                        EntityProvisioned(
                            identifier = EntityIdentifier("zigbee.0x943469fffeb3c843"),
                            type = Light,
                            naming = EntityNaming(
                                key = TranslationKey("light.kitchen"),
                            ),
                            occurredAt = instant,
                        ),
                    )

                query.register(entity = entity())

                client.receive(device)

                runCurrent()
                expectNoEvents()
            }
        }
    }

    test(name = "Renaming a provisioned device should not propose provisioning again") {
        runTest {
            val query = FakeEntityQuery()

            withZigbeeIntegrationAdapter(query = query) { client, _ ->
                client.receive(
                    light(
                        friendlyName = "light.kitchen",
                    ),
                )

                expectThat(awaitItem())
                    .isA<EntityProvisioned>()

                query.register(entity = entity())

                client.receive(
                    light(
                        friendlyName = "light.kitchen-ceiling",
                    ),
                )

                runCurrent()
                expectNoEvents()
            }
        }
    }

    test(name = "Receiving state for a provisioned light should discover the entity") {
        runTest {
            val query = FakeEntityQuery().apply {
                register(entity())
            }

            withZigbeeIntegrationAdapter(query = query) { client, _ ->
                client.receive(light())

                client.receive(
                    topic = "zigbee2mqtt/light.kitchen",
                    payload = """
                        {
                            "state": "ON",
                            "brightness": 127
                        }
                    """.trimIndent(),
                )

                expectThat(awaitItem())
                    .isA<EntityDiscovered>()
                    .get(EntityDiscovered::state)
                    .isA<Light.State.Known>()
                    .with(Light.State.Known::onOff) {
                        isA<Capability.Required<OnOff>>()
                    }
                    .with(Light.State.Known::brightness) {
                        isA<Capability.Available<Brightness>>()
                    }

                runCurrent()
                expectNoEvents()
            }
        }
    }

    test(name = "Receiving state without an optional capability should discover the entity") {
        runTest {
            val query = FakeEntityQuery().apply {
                register(entity())
            }

            withZigbeeIntegrationAdapter(query = query) { client, _ ->
                client.receive(light())

                client.receive(
                    topic = "zigbee2mqtt/light.kitchen",
                    payload = """{"state": "ON"}""",
                )

                expectThat(awaitItem())
                    .isA<EntityDiscovered>()
                    .get(EntityDiscovered::state)
                    .isA<Light.State.Known>()
                    .with(Light.State.Known::onOff) {
                        isA<Capability.Required<OnOff>>()
                    }
                    .with(Light.State.Known::brightness) {
                        isA<Capability.Unsupported>()
                    }

                runCurrent()
                expectNoEvents()
            }
        }
    }

    test(name = "Receiving state without a required capability should not discover the entity") {
        runTest {
            val query = FakeEntityQuery().apply {
                register(entity())
            }

            withZigbeeIntegrationAdapter(query = query) { client, _ ->
                client.receive(light())

                client.receive(
                    topic = "zigbee2mqtt/light.kitchen",
                    payload = """{"brightness": 127}""",
                )

                runCurrent()
                expectNoEvents()
            }
        }
    }

    test(name = "Receiving complementary partial states should discover the entity when all required capabilities become available") {
        runTest {
            val query = FakeEntityQuery().apply {
                register(entity())
            }

            withZigbeeIntegrationAdapter(query = query) { client, _ ->
                client.receive(light())

                client.receive(
                    topic = "zigbee2mqtt/light.kitchen",
                    payload = """{"brightness": 127}""",
                )

                runCurrent()
                expectNoEvents()

                client.receive(
                    topic = "zigbee2mqtt/light.kitchen",
                    payload = """{"state": "ON"}""",
                )

                expectThat(awaitItem())
                    .isA<EntityDiscovered>()
                    .get(EntityDiscovered::state)
                    .isA<Light.State.Known>()
                    .with(Light.State.Known::onOff) {
                        isA<Capability.Required<OnOff>>()
                    }
                    .with(Light.State.Known::brightness) {
                        isA<Capability.Available<Brightness>>()
                    }

                runCurrent()
                expectNoEvents()
            }
        }
    }

    test(name = "Receiving state before successful provisioning should postpone discovery") {
        runTest {
            val query = FakeEntityQuery()

            withZigbeeIntegrationAdapter(query = query) { client, _ ->
                client.receive(light())

                expectThat(awaitItem())
                    .isA<EntityProvisioned>()

                client.receive(
                    topic = "zigbee2mqtt/light.kitchen",
                    payload = """{"state": "ON"}""",
                )

                runCurrent()
                expectNoEvents()

                query.register(entity())

                client.receive(
                    topic = "zigbee2mqtt/light.kitchen",
                    payload = """{"brightness": 127}""",
                )

                expectThat(awaitItem())
                    .isA<EntityDiscovered>()
                    .get(EntityDiscovered::state)
                    .isA<Light.State.Known>()
                    .with(Light.State.Known::onOff) {
                        isA<Capability.Required<OnOff>>()
                    }
                    .with(Light.State.Known::brightness) {
                        isA<Capability.Available<Brightness>>()
                    }

                runCurrent()
                expectNoEvents()
            }
        }
    }

    test(name = "Receiving state for an already discovered entity should not discover the entity again") {
        runTest {
            val query = FakeEntityQuery().apply {
                register(
                    entity(
                        state = Light.State.Known(
                            onOff = required(OnOff(current = true)),
                            brightness = optional(Brightness(current = 50.0.percent)),
                        ),
                    ),
                )
            }

            withZigbeeIntegrationAdapter(query = query) { client, _ ->
                client.receive(light())

                client.receive(
                    topic = "zigbee2mqtt/light.kitchen",
                    payload = """
                        {
                            "state": "OFF",
                            "brightness": 254
                        }
                    """.trimIndent(),
                )

                runCurrent()
                expectNoEvents()
            }
        }
    }

    test(name = "Receiving invalid device state should preserve previously received state and not stop message processing") {
        runTest {
            val query = FakeEntityQuery().apply {
                register(entity())
            }

            withZigbeeIntegrationAdapter(query = query) { client, _ ->
                client.receive(light())

                client.receive(
                    topic = "zigbee2mqtt/light.kitchen",
                    payload = """{"brightness": 127}""",
                )

                runCurrent()
                expectNoEvents()

                client.receive(
                    topic = "zigbee2mqtt/light.kitchen",
                    payload = "invalid-json",
                )

                runCurrent()
                expectNoEvents()

                client.receive(
                    topic = "zigbee2mqtt/light.kitchen",
                    payload = """{"state": "ON"}""",
                )

                expectThat(awaitItem())
                    .isA<EntityDiscovered>()
                    .get(EntityDiscovered::state)
                    .isA<Light.State.Known>()
                    .with(Light.State.Known::onOff) {
                        isA<Capability.Required<OnOff>>()
                    }
                    .with(Light.State.Known::brightness) {
                        isA<Capability.Available<Brightness>>()
                    }

                runCurrent()
                expectNoEvents()
            }
        }
    }

    test(name = "Receiving state for an unknown device should not register its state") {
        runTest {
            val query = FakeEntityQuery().apply {
                register(entity())
            }

            withZigbeeIntegrationAdapter(query = query) { client, _ ->
                client.receive(
                    topic = "zigbee2mqtt/light.kitchen",
                    payload = """{"brightness": 127}""",
                )

                runCurrent()
                expectNoEvents()

                client.receive(light())

                client.receive(
                    topic = "zigbee2mqtt/light.kitchen",
                    payload = """{"state": "ON"}""",
                )

                expectThat(awaitItem())
                    .isA<EntityDiscovered>()
                    .get(EntityDiscovered::state)
                    .isA<Light.State.Known>()
                    .with(Light.State.Known::onOff) {
                        isA<Capability.Required<OnOff>>()
                    }
                    .with(Light.State.Known::brightness) {
                        isA<Capability.Unsupported>()
                    }

                runCurrent()
                expectNoEvents()
            }
        }
    }

    test(name = "Receiving state for an unsupported device should not discover the entity") {
        runTest {
            val query = FakeEntityQuery().apply {
                register(entity())
            }

            withZigbeeIntegrationAdapter(query = query) { client, _ ->
                client.receive(
                    light(supported = false),
                )

                client.receive(
                    topic = "zigbee2mqtt/light.kitchen",
                    payload = """
                        {
                            "state": "ON",
                            "brightness": 127
                        }
                    """.trimIndent(),
                )

                runCurrent()
                expectNoEvents()
            }
        }
    }

    test(name = "Rediscovering a removed device should not use previously received device state") {
        runTest {
            val query = FakeEntityQuery().apply {
                register(entity())
            }

            withZigbeeIntegrationAdapter(query = query) { client, _ ->
                client.receive(light())

                client.receive(
                    topic = "zigbee2mqtt/light.kitchen",
                    payload = """{"brightness": 127}""",
                )

                runCurrent()
                expectNoEvents()

                // Remove all devices from the snapshot.
                client.receive()

                runCurrent()
                expectNoEvents()

                // Register the device again.
                client.receive(light())

                client.receive(
                    topic = "zigbee2mqtt/light.kitchen",
                    payload = """{"state": "ON"}""",
                )

                expectThat(awaitItem())
                    .isA<EntityDiscovered>()
                    .get(EntityDiscovered::state)
                    .isA<Light.State.Known>()
                    .with(Light.State.Known::onOff) {
                        isA<Capability.Required<OnOff>>()
                    }
                    .with(Light.State.Known::brightness) {
                        isA<Capability.Unsupported>()
                    }

                runCurrent()
                expectNoEvents()
            }
        }
    }

    test(name = "Restarting the integration should discard previously received device state") {
        runTest {
            val query = FakeEntityQuery().apply {
                register(entity())
            }

            withZigbeeIntegrationAdapter(query = query) { client, adapter ->
                client.receive(light())

                client.receive(
                    topic = "zigbee2mqtt/light.kitchen",
                    payload = """{"brightness": 127}""",
                )

                runCurrent()
                expectNoEvents()

                adapter.restart()

                expectThat(client.subscription.await())
                    .containsExactly("zigbee2mqtt/#")

                client.receive(
                    topic = "zigbee2mqtt/light.kitchen",
                    payload = """{"state": "OFF"}""",
                )

                runCurrent()
                expectNoEvents()

                client.receive(light())

                client.receive(
                    topic = "zigbee2mqtt/light.kitchen",
                    payload = """{"state": "ON"}""",
                )

                expectThat(awaitItem())
                    .isA<EntityDiscovered>()
                    .get(EntityDiscovered::state)
                    .isA<Light.State.Known>()
                    .with(Light.State.Known::onOff) {
                        isA<Capability.Required<OnOff>>()
                    }
                    .with(Light.State.Known::brightness) {
                        isA<Capability.Unsupported>()
                    }

                runCurrent()
                expectNoEvents()
            }
        }
    }

    test(name = "Receiving state for an entity of a mismatched type should not discover the entity") {
        runTest {
            val query = FakeEntityQuery().apply {
                register(
                    System(
                        identifier = EntityIdentifier("zigbee.0x943469fffeb3c843"),
                        naming = System.naming(),
                        state = System.State.Unknown,
                    ),
                )
            }

            withZigbeeIntegrationAdapter(query = query) { client, _ ->
                client.receive(light())

                client.receive(
                    topic = "zigbee2mqtt/light.kitchen",
                    payload = """
                        {
                            "state": "ON",
                            "brightness": 127
                        }
                    """.trimIndent(),
                )

                runCurrent()
                expectNoEvents()
            }
        }
    }

    test(name = "Receiving state during provisioning should discover the entity after provisioning has completed") {
        runTest {
            val query = FakeEntityQuery()

            val started = CompletableDeferred<Unit>()
            val completed = CompletableDeferred<Unit>()

            val publisher = FakeEntityEventPublisher { event ->
                if (event is EntityProvisioned) {
                    started.complete(Unit)
                    completed.await()
                    query.register(entity())
                }

                EntityEventPublisher.Result.Accepted
            }

            withZigbeeIntegrationAdapter(
                query = query,
                publisher = publisher,
            ) { client, _ ->
                client.receive(light())

                started.await()

                client.receive(
                    topic = "zigbee2mqtt/light.kitchen",
                    payload = """
                        {
                            "state": "ON",
                            "brightness": 127
                        }
                    """.trimIndent(),
                )

                runCurrent()
                expectNoEvents()

                completed.complete(Unit)

                expectThat(awaitItem())
                    .isA<EntityProvisioned>()

                expectThat(awaitItem())
                    .isA<EntityDiscovered>()
                    .get(EntityDiscovered::state)
                    .isA<Light.State.Known>()
                    .with(Light.State.Known::onOff) {
                        isA<Capability.Required<OnOff>>()
                    }
                    .with(Light.State.Known::brightness) {
                        isA<Capability.Available<Brightness>>()
                    }

                runCurrent()
                expectNoEvents()
            }
        }
    }

    test(name = "Receiving state during discovery should not discover the entity again after discovery has completed") {
        runTest {
            val query = FakeEntityQuery().apply {
                register(entity())
            }

            val started = CompletableDeferred<Unit>()
            val completed = CompletableDeferred<Unit>()

            val state = Light.State.Known(
                onOff = required(OnOff(current = true)),
                brightness = optional(
                    Brightness(current = 50.0.percent),
                ),
            )

            val publisher = FakeEntityEventPublisher { event ->
                if (event is EntityDiscovered) {
                    started.complete(Unit)

                    completed.await()

                    query.register(
                        entity = entity(
                            state = state,
                        ),
                    )
                }

                EntityEventPublisher.Result.Accepted
            }

            withZigbeeIntegrationAdapter(
                query = query,
                publisher = publisher,
            ) { client, _ ->
                client.receive(light())

                val payload = """
                    {
                        "state": "ON",
                        "brightness": 127
                    }
                """.trimIndent()

                client.receive(
                    topic = "zigbee2mqtt/light.kitchen",
                    payload = payload,
                )

                started.await()

                client.receive(
                    topic = "zigbee2mqtt/light.kitchen",
                    payload = payload,
                )

                runCurrent()
                expectNoEvents()

                completed.complete(Unit)

                expectThat(awaitItem())
                    .isA<EntityDiscovered>()
                    .get(EntityDiscovered::state)
                    .isA<Light.State.Known>()
                    .with(Light.State.Known::onOff) {
                        isA<Capability.Required<OnOff>>()
                    }
                    .with(Light.State.Known::brightness) {
                        isA<Capability.Available<Brightness>>()
                    }

                runCurrent()
                expectNoEvents()
            }
        }
    }
}

private val identifier =
    IntegrationIdentifier("integration.zigbee")

private val instant: Instant =
    Instant.parse("2026-01-01T12:00:00Z")

private suspend fun TestScope.withZigbeeIntegrationAdapter(
    query: EntityQuery = FakeEntityQuery(),
    publisher: FakeEntityEventPublisher = FakeEntityEventPublisher(),
    block: suspend ReceiveTurbine<Event>.(FakeMqttClient, ZigbeeIntegrationAdapter) -> Unit,
) {
    val client = FakeMqttClient()

    val adapter = ZigbeeIntegrationAdapter(
        identifier = identifier,
        client = client,
        query = query,
        publisher = publisher,
        scope = backgroundScope,
        clock = object : Clock {
            override fun now(): Instant = instant
        },
    )

    publisher.events.test {
        try {
            adapter.start()

            expectThat(client.subscription.await())
                .containsExactly("zigbee2mqtt/#")

            block.invoke(this, client, adapter)
        } finally {
            adapter.stop()
        }
    }
}

private fun light(
    ieeeAddress: String = "0x943469fffeb3c843",
    friendlyName: String = "light.kitchen",
    supported: Boolean = true,
    disabled: Boolean = false,
): String = """
    {
        "ieee_address": "$ieeeAddress",
        "friendly_name": "$friendlyName",
        "supported": $supported,
        "disabled": $disabled,
        "interview_state": "SUCCESSFUL",
        "definition": {
            "model": "ROB_200-011-0",
            "vendor": "ROBB",
            "description": "Zigbee AC phase-cut dimmer",
            "exposes": [
                {
                    "type": "light",
                    "features": [
                        {
                            "type": "binary",
                            "name": "state",
                            "property": "state",
                            "access": 7,
                            "value_on": "ON",
                            "value_off": "OFF"
                        },
                        {
                            "type": "numeric",
                            "name": "brightness",
                            "property": "brightness",
                            "access": 7,
                            "value_min": 0,
                            "value_max": 254
                        }
                    ]
                }
            ]
        }
    }
""".trimIndent()

private fun thermostat(
    ieeeAddress: String = "0xc09b9efffeadb91e",
    friendlyName: String = "thermostat.office",
): String = """
    {
        "ieee_address": "$ieeeAddress",
        "friendly_name": "$friendlyName",
        "supported": true,
        "disabled": false,
        "interview_state": "SUCCESSFUL",
        "definition": {
            "model": "TRVZB",
            "vendor": "SONOFF",
            "description": "Zigbee thermostatic radiator valve",
            "exposes": [
                {
                    "type": "climate",
                    "features": [
                        {
                            "type": "numeric",
                            "name": "local_temperature",
                            "property": "local_temperature",
                            "access": 5,
                            "unit": "°C"
                        }
                    ]
                }
            ]
        }
    }
""".trimIndent()

internal class FakeEntityEventPublisher(
    private val processor: suspend (Event) -> EntityEventPublisher.Result = {
        EntityEventPublisher.Result.Accepted
    },
) : EntityEventPublisher {

    private val channel = Channel<Event>(Channel.UNLIMITED)
    val events: Flow<Event> = channel.receiveAsFlow()

    override suspend fun publish(
        event: Event,
    ): EntityEventPublisher.Result =
        processor(event).also {
            channel.send(element = event)
        }
}

private class FakeEntityQuery : EntityQuery {

    private val entities = mutableMapOf<EntityIdentifier, Entity>()

    fun register(entity: Entity) {
        entities[entity.identifier] = entity
    }

    override suspend fun findByIdentifier(identifier: EntityIdentifier): Entity? =
        entities[identifier]
}
