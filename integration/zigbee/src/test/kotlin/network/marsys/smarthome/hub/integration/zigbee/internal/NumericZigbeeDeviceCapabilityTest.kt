package network.marsys.smarthome.hub.integration.zigbee.internal

import de.infix.testBalloon.framework.core.testSuite
import dev.nmarsman.expect.api.expectThat
import dev.nmarsman.expect.assertions.isEqualTo
import dev.nmarsman.expect.assertions.isNull
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import network.marsys.smarthome.hub.integration.zigbee.api.GenericZigbeeDeviceCapability

val NumericZigbeeDeviceCapabilityTest by testSuite(
    name = "Numeric zigbee device capability tests",
) {
    val capability = GenericZigbeeDeviceCapability.Numeric(
        access = 7,
        name = "brightness",
        property = "brightness",
        valueMin = 0.0,
        valueMax = 254.0,
    )

    testSuite(name = "Resolving values") {
        test(name = "Resolving an integer value should return a double") {
            val state = mapOf(
                "brightness" to JsonPrimitive(127),
            )

            expectThat(capability.resolve(state))
                .isEqualTo(127.0)
        }

        test(name = "Resolving a decimal value should preserve its precision") {
            val state = mapOf(
                "brightness" to JsonPrimitive(12.5),
            )

            expectThat(capability.resolve(state))
                .isEqualTo(12.5)
        }

        test(name = "Resolving a missing value should return null") {
            expectThat(capability.resolve(emptyMap()))
                .isNull()
        }

        test(name = "Resolving a null value should return null") {
            val state = mapOf(
                "brightness" to JsonNull,
            )

            expectThat(capability.resolve(state))
                .isNull()
        }

        test(name = "Resolving a string value should return null") {
            val state = mapOf(
                "brightness" to JsonPrimitive("127"),
            )

            expectThat(capability.resolve(state))
                .isNull()
        }

        test(name = "Resolving a boolean value should return null") {
            val state = mapOf(
                "brightness" to JsonPrimitive(true),
            )

            expectThat(capability.resolve(state))
                .isNull()
        }

        test(name = "Resolving a composite value should return null") {
            val state = mapOf(
                "brightness" to buildJsonObject {
                    put("value", JsonPrimitive(127))
                },
            )

            expectThat(capability.resolve(state))
                .isNull()
        }

        test(name = "Resolving a value should use the property instead of the name") {
            val capability = capability.copy(
                property = "brightness_left",
            )

            val state = mapOf(
                "brightness" to JsonPrimitive(0),
                "brightness_left" to JsonPrimitive(127),
            )

            expectThat(capability.resolve(state))
                .isEqualTo(127.0)
        }

        test(name = "Resolving a value outside the defined range should preserve the value") {
            val state = mapOf(
                "brightness" to JsonPrimitive(255),
            )

            expectThat(capability.resolve(state))
                .isEqualTo(255.0)
        }

        test(name = "Resolving a non-finite value should return null") {
            val state = mapOf(
                "brightness" to JsonPrimitive(Double.NaN),
            )

            expectThat(capability.resolve(state))
                .isNull()
        }
    }

    testSuite(name = "Normalizing values") {
        test(name = "Normalizing the minimum value should return zero") {
            expectThat(capability.normalize(0.0))
                .isEqualTo(0.0)
        }

        test(name = "Normalizing the maximum value should return one") {
            expectThat(capability.normalize(254.0))
                .isEqualTo(1.0)
        }

        test(name = "Normalizing the middle of the range should return one half") {
            expectThat(capability.normalize(127.0))
                .isEqualTo(0.5)
        }

        test(name = "Normalizing a value should respect a non-zero minimum") {
            val capability = capability.copy(
                valueMin = 10.0,
                valueMax = 110.0,
            )

            expectThat(capability.normalize(60.0))
                .isEqualTo(0.5)
        }

        test(name = "Normalizing a value should support negative boundaries") {
            val capability = capability.copy(
                valueMin = -100.0,
                valueMax = 100.0,
            )

            expectThat(capability.normalize(0.0))
                .isEqualTo(0.5)
        }

        test(name = "Normalizing a value below the minimum should return null") {
            expectThat(capability.normalize(-1.0))
                .isNull()
        }

        test(name = "Normalizing a value above the maximum should return null") {
            expectThat(capability.normalize(255.0))
                .isNull()
        }

        test(name = "Normalizing without a minimum should return null") {
            val capability = capability.copy(valueMin = null)

            expectThat(capability.normalize(127.0))
                .isNull()
        }

        test(name = "Normalizing without a maximum should return null") {
            val capability = capability.copy(valueMax = null)

            expectThat(capability.normalize(127.0))
                .isNull()
        }

        test(name = "Normalizing with identical boundaries should return null") {
            val capability = capability.copy(
                valueMin = 100.0,
                valueMax = 100.0,
            )

            expectThat(capability.normalize(100.0))
                .isNull()
        }

        test(name = "Normalizing with reversed boundaries should return null") {
            val capability = capability.copy(
                valueMin = 254.0,
                valueMax = 0.0,
            )

            expectThat(capability.normalize(127.0))
                .isNull()
        }

        test(name = "Normalizing a non-finite minimum should return null") {
            val capability = capability.copy(
                valueMin = Double.NEGATIVE_INFINITY,
            )

            expectThat(capability.normalize(127.0))
                .isNull()
        }

        test(name = "Normalizing a non-finite maximum should return null") {
            val capability = capability.copy(
                valueMax = Double.POSITIVE_INFINITY,
            )

            expectThat(capability.normalize(127.0))
                .isNull()
        }

        test(name = "Normalizing a non-finite value should return null") {
            expectThat(capability.normalize(Double.NaN))
                .isNull()
        }

        test(name = "Normalizing a non-finite range should return null") {
            val capability = capability.copy(
                valueMin = -Double.MAX_VALUE,
                valueMax = Double.MAX_VALUE,
            )

            expectThat(capability.normalize(0.0))
                .isNull()
        }
    }
}
