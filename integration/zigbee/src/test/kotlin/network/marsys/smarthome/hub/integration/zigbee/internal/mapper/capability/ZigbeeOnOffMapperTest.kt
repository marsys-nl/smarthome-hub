package network.marsys.smarthome.hub.integration.zigbee.internal.mapper.capability

import de.infix.testBalloon.framework.core.testSuite
import dev.nmarsman.expect.api.expectThat
import dev.nmarsman.expect.assertions.isEqualTo
import dev.nmarsman.expect.assertions.isNull
import kotlinx.serialization.json.JsonPrimitive
import network.marsys.smarthome.domain.capability.OnOff
import network.marsys.smarthome.hub.integration.zigbee.api.GenericZigbeeDeviceCapability
import network.marsys.smarthome.hub.integration.zigbee.internal.mapper.ZigbeeOnOffMapper

val ZigbeeOnOffMapperTest by testSuite(
    name = "Zigbee on-off mapper tests",
) {
    val capability = GenericZigbeeDeviceCapability.Binary(
        access = 7,
        name = "state",
        property = "state",
        valueOn = JsonPrimitive("ON"),
        valueOff = JsonPrimitive("OFF"),
    )

    test(name = "Mapping an on value should return an enabled OnOff capability") {
        val state = mapOf(
            "state" to JsonPrimitive("ON"),
        )

        expectThat(subject = ZigbeeOnOffMapper.map(capability, state))
            .isEqualTo(OnOff(current = true))
    }

    test(name = "Mapping an off value should return a disabled OnOff capability") {
        val state = mapOf(
            "state" to JsonPrimitive("OFF"),
        )

        expectThat(ZigbeeOnOffMapper.map(capability, state))
            .isEqualTo(OnOff(current = false))
    }

    test(name = "Mapping a missing value should return null") {
        expectThat(ZigbeeOnOffMapper.map(capability, emptyMap()))
            .isNull()
    }

    test(name = "Mapping an unknown value should return null") {
        val state = mapOf(
            "state" to JsonPrimitive("UNKNOWN"),
        )

        expectThat(ZigbeeOnOffMapper.map(capability, state))
            .isNull()
    }

    test(name = "Mapping boolean values should return an OnOff capability") {
        val binaryCapability = capability.copy(
            valueOn = JsonPrimitive(true),
            valueOff = JsonPrimitive(false),
        )

        val state = mapOf(
            "state" to JsonPrimitive(true),
        )

        expectThat(ZigbeeOnOffMapper.map(binaryCapability, state))
            .isEqualTo(OnOff(current = true))
    }

    test(name = "Mapping a capability should not depend on its name") {
        val binaryCapability = capability.copy(
            name = "enabled",
            property = "enabled",
        )

        val state = mapOf(
            "enabled" to JsonPrimitive("ON"),
        )

        expectThat(ZigbeeOnOffMapper.map(binaryCapability, state))
            .isEqualTo(OnOff(current = true))
    }
}
