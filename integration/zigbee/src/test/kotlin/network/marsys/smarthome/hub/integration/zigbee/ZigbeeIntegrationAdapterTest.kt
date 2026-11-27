package network.marsys.smarthome.hub.integration.zigbee

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import de.infix.testBalloon.framework.core.testSuite
import dev.nmarsman.expect.api.expectThat
import dev.nmarsman.expect.assertions.contains
import dev.nmarsman.expect.assertions.containsExactly
import dev.nmarsman.expect.assertions.isA
import dev.nmarsman.expect.assertions.isEqualTo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import network.marsys.smarthome.domain.TranslationKey
import network.marsys.smarthome.domain.entity.EntityIdentifier
import network.marsys.smarthome.domain.entity.EntityNaming
import network.marsys.smarthome.domain.entity.Light
import network.marsys.smarthome.domain.entity.naming
import network.marsys.smarthome.domain.identifiers.IntegrationIdentifier
import network.marsys.smarthome.hub.feature.entity.domain.event.EntityProvisioned
import network.marsys.smarthome.hub.feature.entity.domain.event.Event
import network.marsys.smarthome.hub.integration.zigbee.helper.FakeMqttClient
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

    test(name = "Renaming a device should not change the entity identifier") {
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

    test(name = "Receiving an identical snapshot should propose provisioning again") {
        runTest {
            withZigbeeIntegrationAdapter { client, _ ->
                val device = light()

                client.receive(device)
                val first = awaitItem()

                client.receive(device)
                val second = awaitItem()

                expectThat(second).isEqualTo(first)

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
}

private val identifier =
    IntegrationIdentifier("integration.zigbee")

private val instant: Instant =
    Instant.parse("2026-01-01T12:00:00Z")

private suspend fun TestScope.withZigbeeIntegrationAdapter(
    block: suspend ReceiveTurbine<Event>.(FakeMqttClient, ZigbeeIntegrationAdapter) -> Unit,
) {
    val client = FakeMqttClient()

    val adapter = ZigbeeIntegrationAdapter(
        identifier = identifier,
        client = client,
        scope = backgroundScope,
        clock = object : Clock {
            override fun now(): Instant = instant
        },
    )

    adapter.events.test {
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
