package network.marsys.smarthome.hub.integration.zigbee.internal.mapper.state

import de.infix.testBalloon.framework.core.testSuite
import dev.nmarsman.expect.api.expectThat
import dev.nmarsman.expect.assertions.isEqualTo
import dev.nmarsman.expect.assertions.isNull
import kotlinx.serialization.json.JsonPrimitive
import network.marsys.smarthome.domain.capability.Brightness
import network.marsys.smarthome.domain.capability.Capability.Companion.optional
import network.marsys.smarthome.domain.capability.Capability.Companion.required
import network.marsys.smarthome.domain.capability.OnOff
import network.marsys.smarthome.domain.entity.Light
import network.marsys.smarthome.domain.unit.percent
import network.marsys.smarthome.hub.integration.zigbee.api.CompositeZigbeeDeviceCapability
import network.marsys.smarthome.hub.integration.zigbee.api.GenericZigbeeDeviceCapability
import network.marsys.smarthome.hub.integration.zigbee.api.ZigbeeDevice
import network.marsys.smarthome.hub.integration.zigbee.api.ZigbeeDeviceCapability
import network.marsys.smarthome.hub.integration.zigbee.api.ZigbeeDeviceDefinition
import network.marsys.smarthome.hub.integration.zigbee.api.ZigbeeInterviewState

val ZigbeeLightStateMapperTest by testSuite(
    name = "Zigbee light state mapper tests",
) {
    test(name = "Mapping a dimmable light should return a known state") {
        val state = mapOf(
            "state" to JsonPrimitive("ON"),
            "brightness" to JsonPrimitive(127),
        )

        expectThat(ZigbeeLightStateMapper.map(light(), state))
            .isEqualTo(
                Light.State.Known(
                    onOff = required(OnOff(current = true)),
                    brightness = optional(Brightness(current = 50.0.percent)),
                ),
            )
    }

    test(name = "Mapping a switched off light should preserve its brightness") {
        val state = mapOf(
            "state" to JsonPrimitive("OFF"),
            "brightness" to JsonPrimitive(127),
        )

        expectThat(ZigbeeLightStateMapper.map(light(), state))
            .isEqualTo(
                Light.State.Known(
                    onOff = required(OnOff(current = false)),
                    brightness = optional(
                        Brightness(current = 50.0.percent),
                    ),
                ),
            )
    }

    test(name = "Mapping a non-dimmable light should return unsupported brightness") {
        val device = light(
            features = listOf(onOff),
        )

        val state = mapOf(
            "state" to JsonPrimitive("ON"),
        )

        expectThat(ZigbeeLightStateMapper.map(device, state))
            .isEqualTo(
                Light.State.Known(
                    onOff = required(OnOff(current = true)),
                    brightness = optional(null),
                ),
            )
    }

    test(name = "Mapping a dimmable light without brightness should return unsupported brightness") {
        val state = mapOf(
            "state" to JsonPrimitive("ON"),
        )

        expectThat(ZigbeeLightStateMapper.map(light(), state))
            .isEqualTo(
                Light.State.Known(
                    onOff = required(OnOff(current = true)),
                    brightness = optional(null),
                ),
            )
    }

    test(name = "Mapping a light with an invalid brightness should return unsupported brightness") {
        val state = mapOf(
            "state" to JsonPrimitive("ON"),
            "brightness" to JsonPrimitive(255),
        )

        expectThat(ZigbeeLightStateMapper.map(light(), state))
            .isEqualTo(
                Light.State.Known(
                    onOff = required(OnOff(current = true)),
                    brightness = optional(null),
                ),
            )
    }

    test(name = "Mapping a light without an on-off value should return null") {
        val state = mapOf(
            "brightness" to JsonPrimitive(127),
        )

        expectThat(ZigbeeLightStateMapper.map(light(), state))
            .isNull()
    }

    test(name = "Mapping a light without an on-off feature should return null") {
        val device = light(
            features = listOf(brightness),
        )

        val state = mapOf(
            "state" to JsonPrimitive("ON"),
            "brightness" to JsonPrimitive(127),
        )

        expectThat(ZigbeeLightStateMapper.map(device, state))
            .isNull()
    }

    test(name = "Mapping a light with an invalid on-off value should return null") {
        val state = mapOf(
            "state" to JsonPrimitive("INVALID"),
        )

        expectThat(ZigbeeLightStateMapper.map(light(), state))
            .isNull()
    }

    test(name = "Mapping a device without a definition should return null") {
        val device = light().copy(definition = null)

        expectThat(ZigbeeLightStateMapper.map(device, emptyMap()))
            .isNull()
    }

    test(name = "Mapping a device without a light expose should return null") {
        val device = light(
            exposes = listOf(brightness),
        )

        expectThat(ZigbeeLightStateMapper.map(device, emptyMap()))
            .isNull()
    }

    test(name = "Mapping a device with multiple light exposes should return null") {
        val expose = CompositeZigbeeDeviceCapability(
            type = "light",
            features = listOf(onOff),
        )

        val device = light(
            exposes = listOf(expose, expose.copy()),
        )

        val state = mapOf(
            "state" to JsonPrimitive("ON"),
        )

        expectThat(ZigbeeLightStateMapper.map(device, state))
            .isNull()
    }

    test(name = "Mapping a light should use the configured feature properties") {
        val device = light(
            features = listOf(
                onOff.copy(property = "state_left"),
                brightness.copy(property = "brightness_left"),
            ),
        )

        val state = mapOf(
            "state" to JsonPrimitive("OFF"),
            "brightness" to JsonPrimitive(0),
            "state_left" to JsonPrimitive("ON"),
            "brightness_left" to JsonPrimitive(127),
        )

        expectThat(ZigbeeLightStateMapper.map(device, state))
            .isEqualTo(
                Light.State.Known(
                    onOff = required(OnOff(current = true)),
                    brightness = optional(
                        Brightness(current = 50.0.percent),
                    ),
                ),
            )
    }

    test(name = "Mapping a light with additional exposes should ignore unrelated exposes") {
        val device = light(
            exposes = listOf(
                brightness.copy(
                    name = "power",
                    property = "power",
                ),
                CompositeZigbeeDeviceCapability(
                    type = "switch",
                    features = listOf(onOff),
                ),
                CompositeZigbeeDeviceCapability(
                    type = "light",
                    features = listOf(onOff, brightness),
                ),
            ),
        )

        val state = mapOf(
            "state" to JsonPrimitive("ON"),
            "brightness" to JsonPrimitive(127),
            "power" to JsonPrimitive(12.5),
        )

        expectThat(ZigbeeLightStateMapper.map(device, state))
            .isEqualTo(
                Light.State.Known(
                    onOff = required(OnOff(current = true)),
                    brightness = optional(
                        Brightness(current = 50.0.percent),
                    ),
                ),
            )
    }

    test(name = "Mapping a device with an empty list of exposes should return null") {
        val device = light(
            exposes = emptyList(),
        )

        expectThat(ZigbeeLightStateMapper.map(device, emptyMap()))
            .isNull()
    }

    test(name = "Mapping a device with an unrelated composite expose should return null") {
        val device = light(
            exposes = listOf(
                CompositeZigbeeDeviceCapability(
                    type = "switch",
                    features = listOf(onOff),
                ),
            ),
        )

        val state = mapOf(
            "state" to JsonPrimitive("ON"),
        )

        expectThat(ZigbeeLightStateMapper.map(device, state))
            .isNull()
    }
}

private val onOff = GenericZigbeeDeviceCapability.Binary(
    access = 7,
    name = "state",
    property = "state",
    valueOn = JsonPrimitive("ON"),
    valueOff = JsonPrimitive("OFF"),
    valueToggle = JsonPrimitive("TOGGLE"),
)

private val brightness = GenericZigbeeDeviceCapability.Numeric(
    access = 7,
    name = "brightness",
    property = "brightness",
    valueMin = 0.0,
    valueMax = 254.0,
)

private fun light(
    features: List<ZigbeeDeviceCapability> =
        listOf(onOff, brightness),
    exposes: List<ZigbeeDeviceCapability> =
        listOf(
            CompositeZigbeeDeviceCapability(
                type = "light",
                features = features,
            ),
        ),
): ZigbeeDevice = ZigbeeDevice(
    ieeeAddress = "0x943469fffeb3c843",
    friendlyName = "light.kitchen",
    disabled = false,
    supported = true,
    description = null,
    definition = ZigbeeDeviceDefinition(
        model = "TEST",
        vendor = "TEST",
        description = "Test light",
        exposes = exposes,
    ),
    interviewState = ZigbeeInterviewState.SUCCESSFUL,
)
