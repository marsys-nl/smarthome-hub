package network.marsys.smarthome.hub.integration.zigbee.internal.mapper.capability

import de.infix.testBalloon.framework.core.testSuite
import dev.nmarsman.expect.api.expectThat
import dev.nmarsman.expect.assertions.isEqualTo
import dev.nmarsman.expect.assertions.isNull
import kotlinx.serialization.json.JsonPrimitive
import network.marsys.smarthome.domain.capability.Brightness
import network.marsys.smarthome.domain.unit.percent
import network.marsys.smarthome.hub.integration.zigbee.api.GenericZigbeeDeviceCapability
import network.marsys.smarthome.hub.integration.zigbee.internal.mapper.ZigbeeBrightnessMapper

val ZigbeeBrightnessMapperTest by testSuite(
    name = "Zigbee brightness mapper tests",
) {
    val capability = GenericZigbeeDeviceCapability.Numeric(
        access = 7,
        name = "brightness",
        property = "brightness",
        valueMin = 0.0,
        valueMax = 254.0,
    )

    test(name = "Mapping the minimum brightness should return zero percent") {
        val state = mapOf(
            "brightness" to JsonPrimitive(0),
        )

        expectThat(ZigbeeBrightnessMapper.map(capability, state))
            .isEqualTo(Brightness(current = 0.0.percent))
    }

    test(name = "Mapping half brightness should return fifty percent") {
        val state = mapOf(
            "brightness" to JsonPrimitive(127),
        )

        expectThat(ZigbeeBrightnessMapper.map(capability, state))
            .isEqualTo(Brightness(current = 50.0.percent))
    }

    test(name = "Mapping the maximum brightness should return one hundred percent") {
        val state = mapOf(
            "brightness" to JsonPrimitive(254),
        )

        expectThat(ZigbeeBrightnessMapper.map(capability, state))
            .isEqualTo(Brightness(current = 100.0.percent))
    }

    test(name = "Mapping brightness without a value should return null") {
        expectThat(ZigbeeBrightnessMapper.map(capability, emptyMap()))
            .isNull()
    }

    test(name = "Mapping brightness outside the supported range should return null") {
        val state = mapOf(
            "brightness" to JsonPrimitive(255),
        )

        expectThat(ZigbeeBrightnessMapper.map(capability, state))
            .isNull()
    }

    test(name = "Mapping brightness should respect the capability boundaries") {
        val capability = capability.copy(
            valueMin = 10.0,
            valueMax = 110.0,
        )

        val state = mapOf(
            "brightness" to JsonPrimitive(35),
        )

        expectThat(ZigbeeBrightnessMapper.map(capability, state))
            .isEqualTo(Brightness(current = 25.0.percent))
    }

    test(name = "Mapping brightness should use the configured property") {
        val capability = capability.copy(
            property = "brightness_left",
        )

        val state = mapOf(
            "brightness" to JsonPrimitive(254),
            "brightness_left" to JsonPrimitive(127),
        )

        expectThat(ZigbeeBrightnessMapper.map(capability, state))
            .isEqualTo(Brightness(current = 50.0.percent))
    }
}
