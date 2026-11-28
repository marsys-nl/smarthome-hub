package network.marsys.smarthome.hub.integration.zigbee.internal

import de.infix.testBalloon.framework.core.testSuite
import dev.nmarsman.expect.api.expectThat
import dev.nmarsman.expect.assertions.isEqualTo
import dev.nmarsman.expect.assertions.isNull
import network.marsys.smarthome.hub.integration.zigbee.api.ZigbeeDevice
import network.marsys.smarthome.hub.integration.zigbee.findByTopic
import network.marsys.smarthome.hub.integration.zigbee.helper.light

val ZigbeeDeviceStateTopicTest by testSuite(
    name = "Zigbee device state topic tests",
) {
    val device = light()
        .copy(
            friendlyName = "light.kitchen",
        )

    test(name = "Finding a device by its state topic should return the device") {
        val registry = registryWith(device)

        expectThat(registry.findByTopic(topic = "zigbee2mqtt/light.kitchen"))
            .isEqualTo(device)
    }

    test(name = "Finding a device by an unknown state topic should return null") {
        val registry = registryWith(device)

        expectThat(registry.findByTopic(topic = "zigbee2mqtt/light.unknown"))
            .isNull()
    }

    test(name = "Finding a device using an incorrect base topic should return null") {
        val registry = registryWith(device)

        expectThat(registry.findByTopic(topic = "other/light.kitchen"))
            .isNull()
    }

    test(name = "Finding a device using a set topic should return null") {
        val registry = registryWith(device)

        expectThat(registry.findByTopic(topic = "zigbee2mqtt/light.kitchen/set"))
            .isNull()
    }

    test(name = "Finding a device using a get topic should return null") {
        val registry = registryWith(device)

        expectThat(registry.findByTopic(topic = "zigbee2mqtt/light.kitchen/get"))
            .isNull()
    }

    test(name = "Finding a device using an availability topic should return null") {
        val registry = registryWith(device)

        expectThat(registry.findByTopic(topic = "zigbee2mqtt/light.kitchen/availability"))
            .isNull()
    }

    test(name = "Finding a device with a hierarchical friendly name should return the device") {
        val device = device.copy(
            friendlyName = "first-floor/bedroom/light",
        )

        val registry = registryWith(device)

        expectThat(registry.findByTopic(topic = "zigbee2mqtt/first-floor/bedroom/light"))
            .isEqualTo(device)
    }

    test(name = "Finding a device with its IEEE address as friendly name should return the device") {
        val device = device.copy(
            friendlyName = device.ieeeAddress,
        )

        val registry = registryWith(device)

        expectThat(registry.findByTopic(topic = "zigbee2mqtt/${device.ieeeAddress}"))
            .isEqualTo(device)
    }

    test(name = "Finding a renamed device should only succeed using its new friendly name") {
        val registry = registryWith(device)

        val renamedDevice = device.copy(
            friendlyName = "light.dining-room",
        )

        registry.reconcile(listOf(renamedDevice))

        expectThat(registry.findByTopic(topic = "zigbee2mqtt/light.kitchen"))
            .isNull()

        expectThat(registry.findByTopic(topic = "zigbee2mqtt/light.dining-room"))
            .isEqualTo(renamedDevice)
    }
}

private fun registryWith(
    device: ZigbeeDevice,
): ZigbeeDeviceRegistry =
    ZigbeeDeviceRegistry().apply {
        reconcile(listOf(device))
    }
