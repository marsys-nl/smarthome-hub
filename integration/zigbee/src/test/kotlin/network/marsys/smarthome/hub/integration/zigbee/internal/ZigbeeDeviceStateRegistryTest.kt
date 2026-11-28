package network.marsys.smarthome.hub.integration.zigbee.internal

import de.infix.testBalloon.framework.core.testSuite
import dev.nmarsman.expect.api.expectDoesNotThrow
import dev.nmarsman.expect.api.expectThat
import dev.nmarsman.expect.assertions.isEmpty
import dev.nmarsman.expect.assertions.isEqualTo
import dev.nmarsman.expect.assertions.isNull
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

val ZigbeeDeviceStateRegistryTest by testSuite(
    name = "Zigbee device state registry tests",
) {
    val ieeeAddress = "0x943469fffeb3c843"

    val initial = mapOf(
        "state" to JsonPrimitive("ON"),
        "brightness" to JsonPrimitive(127),
    )

    test(name = "Finding an unknown device state should return null") {
        val registry = ZigbeeDeviceStateRegistry()

        expectThat(registry.findByIeeeAddress(ieeeAddress))
            .isNull()
    }

    test(name = "Updating an unknown device should register its state") {
        val registry = ZigbeeDeviceStateRegistry()

        expectThat(registry.update(ieeeAddress, initial))
            .isEqualTo(initial)

        expectThat(registry.findByIeeeAddress(ieeeAddress))
            .isEqualTo(initial)
    }

    test(name = "Updating an existing property should replace its value") {
        val registry = ZigbeeDeviceStateRegistry()

        registry.update(ieeeAddress, initial)

        val state = registry.update(
            ieeeAddress,
            mapOf("brightness" to JsonPrimitive(200)),
        )

        expectThat(state).isEqualTo(
            mapOf(
                "state" to JsonPrimitive("ON"),
                "brightness" to JsonPrimitive(200),
            ),
        )
    }

    test(name = "Updating a device with a new property should extend its state") {
        val registry = ZigbeeDeviceStateRegistry()

        registry.update(ieeeAddress, initial)

        val state = registry.update(
            ieeeAddress,
            mapOf("power" to JsonPrimitive(12.4)),
        )

        expectThat(state).isEqualTo(
            initial + ("power" to JsonPrimitive(12.4)),
        )
    }

    test(name = "Updating a property with null should replace its previous value") {
        val registry = ZigbeeDeviceStateRegistry()

        registry.update(ieeeAddress, initial)

        val state = registry.update(
            ieeeAddress,
            mapOf("brightness" to JsonNull),
        )

        expectThat(state).isEqualTo(
            mapOf(
                "state" to JsonPrimitive("ON"),
                "brightness" to JsonNull,
            ),
        )
    }

    test(name = "Updating an existing device with an empty state should preserve its state") {
        val registry = ZigbeeDeviceStateRegistry()

        registry.update(ieeeAddress, initial)

        expectThat(registry.update(ieeeAddress, emptyMap()))
            .isEqualTo(initial)

        expectThat(registry.findByIeeeAddress(ieeeAddress))
            .isEqualTo(initial)
    }

    test(name = "Updating an unknown device with an empty state should not register a state") {
        val registry = ZigbeeDeviceStateRegistry()

        expectThat(registry.update(ieeeAddress, emptyMap()))
            .isEmpty()

        expectThat(registry.findByIeeeAddress(ieeeAddress))
            .isNull()
    }

    test(name = "Updating a device should not affect other device states") {
        val registry = ZigbeeDeviceStateRegistry()
        val otherAddress = "0x385b44fffe18236e"

        registry.update(ieeeAddress, initial)
        registry.update(otherAddress, initial)

        registry.update(
            ieeeAddress,
            mapOf("state" to JsonPrimitive("OFF")),
        )

        expectThat(registry.findByIeeeAddress(otherAddress))
            .isEqualTo(initial)

        expectThat(registry.findByIeeeAddress(ieeeAddress))
            .isEqualTo(
                initial + ("state" to JsonPrimitive("OFF")),
            )
    }

    test(name = "Updating a device should not mutate previously returned states") {
        val registry = ZigbeeDeviceStateRegistry()

        val previous = registry.update(
            ieeeAddress,
            initial,
        )

        registry.update(
            ieeeAddress,
            mapOf("brightness" to JsonPrimitive(200)),
        )

        expectThat(previous).isEqualTo(initial)
    }

    test(name = "Removing a device should remove its state") {
        val registry = ZigbeeDeviceStateRegistry()

        registry.update(ieeeAddress, initial)
        registry.remove(ieeeAddress)

        expectThat(registry.findByIeeeAddress(ieeeAddress))
            .isNull()
    }

    test(name = "Removing an unknown device should not throw an exception") {
        val registry = ZigbeeDeviceStateRegistry()

        expectDoesNotThrow {
            registry.remove(ieeeAddress)
        }
    }

    test(name = "Clearing the registry should remove all device states") {
        val registry = ZigbeeDeviceStateRegistry()
        val otherAddress = "0x385b44fffe18236e"

        registry.update(ieeeAddress, initial)
        registry.update(otherAddress, initial)

        registry.clear()

        expectThat(registry.findByIeeeAddress(ieeeAddress))
            .isNull()

        expectThat(registry.findByIeeeAddress(otherAddress))
            .isNull()
    }

    test(name = "Updating a composite property should replace its entire value") {
        val registry = ZigbeeDeviceStateRegistry()

        registry.update(
            ieeeAddress,
            mapOf(
                "color" to buildJsonObject {
                    put("x", 0.32)
                    put("y", 0.45)
                },
            ),
        )

        val updated = mapOf(
            "color" to buildJsonObject {
                put("x", 0.50)
            },
        )

        expectThat(registry.update(ieeeAddress, updated))
            .isEqualTo(updated)
    }
}
