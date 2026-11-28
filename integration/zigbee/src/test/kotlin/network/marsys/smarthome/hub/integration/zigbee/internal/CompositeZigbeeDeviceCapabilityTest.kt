package network.marsys.smarthome.hub.integration.zigbee.internal

import de.infix.testBalloon.framework.core.testSuite
import dev.nmarsman.expect.api.expectThat
import dev.nmarsman.expect.assertions.isEqualTo
import dev.nmarsman.expect.assertions.isNull
import kotlinx.serialization.json.JsonPrimitive
import network.marsys.smarthome.domain.capability.OnOff
import network.marsys.smarthome.hub.integration.zigbee.api.CompositeZigbeeDeviceCapability
import network.marsys.smarthome.hub.integration.zigbee.api.GenericZigbeeDeviceCapability
import network.marsys.smarthome.hub.integration.zigbee.helper.brightness
import network.marsys.smarthome.hub.integration.zigbee.helper.onOff
import network.marsys.smarthome.hub.integration.zigbee.internal.mapper.ZigbeeOnOffMapper

val CompositeZigbeeDeviceCapabilityTest by testSuite(
    name = "Composite Zigbee device capability tests",
) {
    val light = CompositeZigbeeDeviceCapability(
        type = "light",
        features = listOf(
            onOff,
            brightness,
        ),
    )

    test(name = "Finding a feature by type and predicate should return the feature") {
        val feature = light.find<GenericZigbeeDeviceCapability.Binary> {
            it.name == "state"
        }

        expectThat(feature)
            .isEqualTo(onOff)
    }

    test(name = "Finding a feature of an incorrect type should return null") {
        val feature = light.find<GenericZigbeeDeviceCapability.Numeric> {
            it.name == "state"
        }

        expectThat(feature)
            .isNull()
    }

    test(name = "Finding a feature without a matching name should return null") {
        val feature = light.find<GenericZigbeeDeviceCapability.Binary> {
            it.name == "unknown"
        }

        expectThat(feature)
            .isNull()
    }

    test(name = "Finding multiple matching features should return null") {
        val light = light.copy(
            features = listOf(
                onOff,
                onOff.copy(property = "state_right"),
            ),
        )

        val feature = light.find<GenericZigbeeDeviceCapability.Binary> {
            it.name == "state"
        }

        expectThat(feature).isNull()
    }

    test(name = "Mapping a feature should return the mapped capability") {
        val capability = light.feature(
            mapper = ZigbeeOnOffMapper,
            state = mapOf("state" to JsonPrimitive("ON")),
        ) {
            it.name == "state"
        }

        expectThat(capability)
            .isEqualTo(OnOff(current = true))
    }

    test(name = "Mapping a non-existing feature should return null") {
        val capability = light.feature(
            mapper = ZigbeeOnOffMapper,
            state = mapOf("state" to JsonPrimitive("ON")),
        ) {
            it.name == "unknown"
        }

        expectThat(capability)
            .isNull()
    }
}
