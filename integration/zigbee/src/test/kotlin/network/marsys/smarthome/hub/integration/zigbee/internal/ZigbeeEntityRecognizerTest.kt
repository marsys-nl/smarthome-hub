package network.marsys.smarthome.hub.integration.zigbee.internal

import de.infix.testBalloon.framework.core.testSuite
import dev.nmarsman.expect.api.expectThat
import dev.nmarsman.expect.assertions.isEqualTo
import dev.nmarsman.expect.assertions.isNull
import network.marsys.smarthome.domain.entity.Light
import network.marsys.smarthome.hub.integration.zigbee.api.CompositeZigbeeDeviceCapability
import network.marsys.smarthome.hub.integration.zigbee.api.GenericZigbeeDeviceCapability
import network.marsys.smarthome.hub.integration.zigbee.api.ZigbeeDevice
import network.marsys.smarthome.hub.integration.zigbee.api.ZigbeeDeviceCapability
import network.marsys.smarthome.hub.integration.zigbee.api.ZigbeeDeviceDefinition
import network.marsys.smarthome.hub.integration.zigbee.api.ZigbeeInterviewState

val ZigbeeEntityRecognizerTest by testSuite(
    name = "Zigbee entity recognizer tests",
) {
    val recognizer = ZigbeeEntityRecognizer()

    test(name = "Recognizing a light should return the Light entity type") {
        val device = device(exposes = listOf(light))

        expectThat(recognizer.recognize(device))
            .isEqualTo(Light)
    }

    test(name = "Recognizing an unsupported light should return null") {
        val device = device(
            exposes = listOf(light),
        ).copy(
            supported = false,
        )

        expectThat(recognizer.recognize(device))
            .isNull()
    }

    test(name = "Recognizing a disabled light should return null") {
        val device = device(
            exposes = listOf(light),
        ).copy(
            disabled = true,
        )

        expectThat(recognizer.recognize(device))
            .isNull()
    }

    test(name = "Recognizing a light with a failed interview should return null") {
        val device = device(
            exposes = listOf(light),
        ).copy(
            interviewState = ZigbeeInterviewState.FAILED,
        )

        expectThat(recognizer.recognize(device))
            .isNull()
    }

    test(name = "Recognizing a device without a definition should return null") {
        val device = device().copy(
            definition = null,
        )

        expectThat(recognizer.recognize(device))
            .isNull()
    }

    test(name = "Recognizing a device without capabilities should return null") {
        val device = device(exposes = emptyList())

        expectThat(recognizer.recognize(device))
            .isNull()
    }

    test(name = "Recognizing a device with an unrecognized capability should return null") {
        val device = device(
            exposes = listOf(
                CompositeZigbeeDeviceCapability(
                    type = "switch",
                    features = emptyList(),
                ),
            ),
        )

        expectThat(recognizer.recognize(device))
            .isNull()
    }

    test(name = "Recognizing a light with additional capabilities should return the Light entity type") {
        val device = device(
            exposes = listOf(
                power,
                light,
                linkQuality,
            ),
        )

        expectThat(recognizer.recognize(device))
            .isEqualTo(Light)
    }

    test(name = "Recognizing a light should not depend on its friendly name") {
        val device = device(
            exposes = listOf(light),
        ).copy(
            friendlyName = "0x943469fffeb3c843",
        )

        expectThat(recognizer.recognize(device))
            .isEqualTo(Light)
    }

    test(name = "Recognizing a device should not infer its type from its friendly name") {
        val device = device(
            exposes = listOf(power),
        ).copy(
            friendlyName = "light.kitchen",
        )

        expectThat(recognizer.recognize(device))
            .isNull()
    }
}

private fun device(
    exposes: List<ZigbeeDeviceCapability> = emptyList(),
): ZigbeeDevice = ZigbeeDevice(
    ieeeAddress = "0x943469fffeb3c843",
    friendlyName = "test-device",
    disabled = false,
    supported = true,
    description = null,
    definition = ZigbeeDeviceDefinition(
        model = "TEST",
        vendor = "Test vendor",
        description = "Test device",
        exposes = exposes,
    ),
    interviewState = ZigbeeInterviewState.SUCCESSFUL,
)

private val light = CompositeZigbeeDeviceCapability(
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
)

private val power = GenericZigbeeDeviceCapability(
    type = "numeric",
    access = 5,
    name = "power",
    property = "power",
)

private val linkQuality = GenericZigbeeDeviceCapability(
    type = "numeric",
    access = 1,
    name = "linkquality",
    property = "linkquality",
)
