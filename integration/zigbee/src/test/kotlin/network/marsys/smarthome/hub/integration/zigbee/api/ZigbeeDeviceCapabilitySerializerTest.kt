package network.marsys.smarthome.hub.integration.zigbee.api

import de.infix.testBalloon.framework.core.testSuite
import dev.nmarsman.expect.api.expectThat
import dev.nmarsman.expect.api.expectThrows
import dev.nmarsman.expect.assertions.contains
import dev.nmarsman.expect.assertions.containsExactly
import dev.nmarsman.expect.assertions.isEqualTo
import dev.nmarsman.expect.assertions.single
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

val ZigbeeDeviceCapabilitySerializerTest by testSuite(
    name = "Zigbee device capability serializer tests",
) {
    val json = Json {
        ignoreUnknownKeys = true
    }

    test(name = "Deserializing a generic capability should return a generic capability") {
        val payload = """
            {
                "type": "binary",
                "access": 1,
                "name": "smoke",
                "label": "Smoke",
                "property": "smoke"
            }
        """.trimIndent()

        val capability = json.decodeFromString<ZigbeeDeviceCapability>(
            string = payload,
        )

        expectThat(capability)
            .isEqualTo(
                GenericZigbeeDeviceCapability(
                    type = "binary",
                    access = 1,
                    name = "smoke",
                    label = "Smoke",
                    property = "smoke",
                ),
            )
    }

    test(name = "Deserializing a generic capability should ignore unknown properties") {
        val payload = """
            {
                "type": "numeric",
                "access": 7,
                "name": "brightness",
                "property": "brightness",
                "value_min": 0,
                "value_max": 254,
                "value_step": 1
            }
        """.trimIndent()

        val capability = json.decodeFromString<ZigbeeDeviceCapability>(
            string = payload,
        )

        expectThat(capability)
            .isEqualTo(
                GenericZigbeeDeviceCapability(
                    type = "numeric",
                    access = 7,
                    name = "brightness",
                    property = "brightness",
                ),
            )
    }

    test(name = "Deserializing a composed capability should return a composed capability") {
        val payload = """
            {
                "type": "light",
                "features": [
                    {
                        "type": "binary",
                        "access": 7,
                        "name": "state",
                        "property": "state",
                        "value_on": "ON",
                        "value_off": "OFF"
                    },
                    {
                        "type": "numeric",
                        "access": 7,
                        "name": "brightness",
                        "property": "brightness"
                    }
                ]
            }
        """.trimIndent()

        val capability = json.decodeFromString<ZigbeeDeviceCapability>(
            string = payload,
        )

        expectThat(capability)
            .isEqualTo(
                CompositeZigbeeDeviceCapability(
                    type = "light",
                    features = listOf(
                        GenericZigbeeDeviceCapability(
                            type = "binary",
                            access = 7,
                            name = "state",
                            property = "state",
                        ),
                        GenericZigbeeDeviceCapability(
                            type = "numeric",
                            access = 7,
                            name = "brightness",
                            property = "brightness",
                        ),
                    ),
                ),
            )
    }

    test(name = "Deserializing a composed capability without features should not be assumed valid") {
        val payload = """
            {
                "type": "light"
            }
        """.trimIndent()

        expectThrows<SerializationException> {
            json.decodeFromString<ZigbeeDeviceCapability>(string = payload)
        }
    }

    test(name = "Deserializing a composed capability with an empty feature list should succeed") {
        val payload = """
            {
                "type": "light",
                "features": []
            }
        """.trimIndent()

        val capability = json.decodeFromString<ZigbeeDeviceCapability>(
            string = payload,
        )

        expectThat(capability)
            .isEqualTo(
                CompositeZigbeeDeviceCapability(
                    type = "light",
                    features = emptyList(),
                ),
            )
    }

    test(name = "Deserializing nested composed capabilities should succeed") {
        val payload = """
            {
                "type": "light",
                "features": [
                    {
                        "type": "composite",
                        "name": "color_xy",
                        "property": "color",
                        "features": [
                            {
                                "type": "numeric",
                                "name": "x",
                                "property": "x",
                                "access": 7
                            },
                            {
                                "type": "numeric",
                                "name": "y",
                                "property": "y",
                                "access": 7
                            }
                        ]
                    }
                ]
            }
        """.trimIndent()

        val capability = json.decodeFromString<ZigbeeDeviceCapability>(
            string = payload,
        )

        expectThat(capability)
            .isEqualTo(
                CompositeZigbeeDeviceCapability(
                    type = "light",
                    features = listOf(
                        CompositeZigbeeDeviceCapability(
                            type = "composite",
                            name = "color_xy",
                            property = "color",
                            features = listOf(
                                GenericZigbeeDeviceCapability(
                                    type = "numeric",
                                    name = "x",
                                    property = "x",
                                    access = 7,
                                ),
                                GenericZigbeeDeviceCapability(
                                    type = "numeric",
                                    name = "y",
                                    property = "y",
                                    access = 7,
                                ),
                            ),
                        ),
                    ),
                ),
            )
    }

    test(name = "Deserializing a list of mixed capabilities should succeed") {
        val payload = """
            [
                {
                    "type": "light",
                    "features": [
                        {
                            "type": "binary",
                            "name": "state",
                            "property": "state",
                            "access": 7
                        }
                    ]
                },
                {
                    "type": "numeric",
                    "name": "power",
                    "property": "power",
                    "access": 5,
                    "unit": "W"
                }
            ]
        """.trimIndent()

        val capabilities = json.decodeFromString<List<ZigbeeDeviceCapability>>(
            string = payload,
        )

        expectThat(capabilities)
            .contains(
                listOf(
                    CompositeZigbeeDeviceCapability(
                        type = "light",
                        features = listOf(
                            GenericZigbeeDeviceCapability(
                                type = "binary",
                                name = "state",
                                property = "state",
                                access = 7,
                            ),
                        ),
                    ),
                    GenericZigbeeDeviceCapability(
                        type = "numeric",
                        name = "power",
                        property = "power",
                        access = 5,
                    ),
                ),
            )
    }
}
