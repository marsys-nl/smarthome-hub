package network.marsys.smarthome.hub.integration.zigbee.internal

import de.infix.testBalloon.framework.core.testSuite
import dev.nmarsman.expect.api.expectThat
import dev.nmarsman.expect.assertions.containsExactly
import dev.nmarsman.expect.assertions.isEmpty
import dev.nmarsman.expect.assertions.isEqualTo
import dev.nmarsman.expect.assertions.isFalse
import dev.nmarsman.expect.assertions.isNull
import dev.nmarsman.expect.assertions.isTrue
import network.marsys.smarthome.hub.integration.zigbee.api.ZigbeeDevice
import network.marsys.smarthome.hub.integration.zigbee.api.ZigbeeDeviceDefinition
import network.marsys.smarthome.hub.integration.zigbee.api.ZigbeeInterviewState
import kotlin.collections.listOf

val ZigbeeDeviceRegistryTest by testSuite(
    name = "Zigbee device registry tests",
) {
    test(name = "Reconciling an initial snapshot should add all devices") {
        val registry = ZigbeeDeviceRegistry()
        val first = device("0x001")
        val second = device("0x002")

        val changes = registry.reconcile(
            snapshot = listOf(first, second),
        )

        expectThat(subject = changes) {
            get(ZigbeeDeviceChanges::added)
                .containsExactly(listOf(first, second))

            get(ZigbeeDeviceChanges::updated)
                .isEmpty()

            get(ZigbeeDeviceChanges::removed)
                .isEmpty()

            get(ZigbeeDeviceChanges::isEmpty)
                .isFalse()
        }
    }

    test(name = "Reconciling an empty initial snapshot should not result in changes") {
        val registry = ZigbeeDeviceRegistry()

        val changes = registry.reconcile(
            snapshot = emptyList(),
        )

        expectThat(subject = changes) {
            get(ZigbeeDeviceChanges::added)
                .isEmpty()

            get(ZigbeeDeviceChanges::updated)
                .isEmpty()

            get(ZigbeeDeviceChanges::removed)
                .isEmpty()

            get(ZigbeeDeviceChanges::isEmpty)
                .isTrue()
        }
    }

    test(name = "Reconciling an identical snapshot should not result in changes") {
        val registry = ZigbeeDeviceRegistry()
        val device = device("0x001")

        registry.reconcile(
            snapshot = listOf(device),
        )

        val changes = registry.reconcile(
            snapshot = listOf(
                device.copy(),
            ),
        )

        expectThat(subject = changes) {
            get(ZigbeeDeviceChanges::added)
                .isEmpty()

            get(ZigbeeDeviceChanges::updated)
                .isEmpty()

            get(ZigbeeDeviceChanges::removed)
                .isEmpty()

            get(ZigbeeDeviceChanges::isEmpty)
                .isTrue()
        }
    }

    test(name = "Reconciling devices in a different order should not result in changes") {
        val registry = ZigbeeDeviceRegistry()
        val first = device("0x001")
        val second = device("0x002")

        registry.reconcile(
            snapshot = listOf(first, second),
        )

        val changes = registry.reconcile(
            snapshot = listOf(second, first),
        )

        expectThat(subject = changes) {
            get(ZigbeeDeviceChanges::added)
                .isEmpty()

            get(ZigbeeDeviceChanges::updated)
                .isEmpty()

            get(ZigbeeDeviceChanges::removed)
                .isEmpty()

            get(ZigbeeDeviceChanges::isEmpty)
                .isTrue()
        }
    }

    test(name = "Reconciling a snapshot with a new device should add only that device") {
        val registry = ZigbeeDeviceRegistry()
        val existing = device("0x001")
        val added = device("0x002")

        registry.reconcile(
            snapshot = listOf(existing),
        )

        val changes = registry.reconcile(
            snapshot = listOf(existing, added),
        )

        expectThat(subject = changes) {
            get(ZigbeeDeviceChanges::added)
                .containsExactly(listOf(added))

            get(ZigbeeDeviceChanges::updated)
                .isEmpty()

            get(ZigbeeDeviceChanges::removed)
                .isEmpty()

            get(ZigbeeDeviceChanges::isEmpty)
                .isFalse()
        }
    }

    test(name = "Reconciling a renamed device should update the existing device") {
        val registry = ZigbeeDeviceRegistry()
        val original = device(
            ieeeAddress = "0x001",
            friendlyName = "light.kitchen",
        )

        val renamed = original.copy(
            friendlyName = "light.kitchen-ceiling",
        )

        registry.reconcile(
            snapshot = listOf(original),
        )

        val changes = registry.reconcile(
            snapshot = listOf(renamed),
        )

        expectThat(subject = changes) {
            get(ZigbeeDeviceChanges::added)
                .isEmpty()

            get(ZigbeeDeviceChanges::updated)
                .containsExactly(listOf(renamed))

            get(ZigbeeDeviceChanges::removed)
                .isEmpty()

            get(ZigbeeDeviceChanges::isEmpty)
                .isFalse()
        }
    }

    test(name = "Reconciling a device with a changed definition should update the device") {
        val registry = ZigbeeDeviceRegistry()
        val original = device("0x001")
        val modified = original.copy(
            definition = ZigbeeDeviceDefinition(
                model = "TEST",
                vendor = "Test Vendor",
                description = "Test device",
                exposes = emptyList(),
            ),
        )

        registry.reconcile(
            snapshot = listOf(original),
        )

        val changes = registry.reconcile(
            snapshot = listOf(modified),
        )

        expectThat(subject = changes) {
            get(ZigbeeDeviceChanges::added)
                .isEmpty()

            get(ZigbeeDeviceChanges::updated)
                .containsExactly(listOf(modified))

            get(ZigbeeDeviceChanges::removed)
                .isEmpty()

            get(ZigbeeDeviceChanges::isEmpty)
                .isFalse()
        }
    }

    test(name = "Reconciling a disabled device should update the device") {
        val registry = ZigbeeDeviceRegistry()
        val original = device("0x001")
        val disabled = original.copy(disabled = true)

        registry.reconcile(
            snapshot = listOf(original),
        )

        val changes = registry.reconcile(
            snapshot = listOf(disabled),
        )

        expectThat(subject = changes) {
            get(ZigbeeDeviceChanges::added)
                .isEmpty()

            get(ZigbeeDeviceChanges::updated)
                .containsExactly(listOf(disabled))

            get(ZigbeeDeviceChanges::removed)
                .isEmpty()

            get(ZigbeeDeviceChanges::isEmpty)
                .isFalse()
        }
    }

    test(name = "Reconciling a snapshot without an existing device should remove that device") {
        val registry = ZigbeeDeviceRegistry()
        val first = device("0x001")
        val second = device("0x002")

        registry.reconcile(
            snapshot = listOf(first, second),
        )

        val changes = registry.reconcile(
            snapshot = listOf(second),
        )

        expectThat(subject = changes) {
            get(ZigbeeDeviceChanges::added)
                .isEmpty()

            get(ZigbeeDeviceChanges::updated)
                .isEmpty()

            get(ZigbeeDeviceChanges::removed)
                .containsExactly(listOf(first))

            get(ZigbeeDeviceChanges::isEmpty)
                .isFalse()
        }
    }

    test(name = "Reconciling an empty snapshot should remove all existing devices") {
        val registry = ZigbeeDeviceRegistry()
        val first = device("0x001")
        val second = device("0x002")

        registry.reconcile(
            snapshot = listOf(first, second),
        )

        val changes = registry.reconcile(
            snapshot = emptyList(),
        )

        expectThat(subject = changes) {
            get(ZigbeeDeviceChanges::added)
                .isEmpty()

            get(ZigbeeDeviceChanges::updated)
                .isEmpty()

            get(ZigbeeDeviceChanges::removed)
                .containsExactly(listOf(first, second))

            get(ZigbeeDeviceChanges::isEmpty)
                .isFalse()
        }
    }

    test(name = "Reconciling an unsupported device should add the device") {
        val registry = ZigbeeDeviceRegistry()
        val unsupported = device("0x001").copy(
            supported = false,
        )

        val changes = registry.reconcile(
            snapshot = listOf(unsupported),
        )

        expectThat(subject = changes) {
            get(ZigbeeDeviceChanges::added)
                .containsExactly(listOf(unsupported))

            get(ZigbeeDeviceChanges::updated)
                .isEmpty()

            get(ZigbeeDeviceChanges::removed)
                .isEmpty()

            get(ZigbeeDeviceChanges::isEmpty)
                .isFalse()
        }
    }

    test(name = "Reconciling a snapshot should handle simultaneous device changes") {
        val registry = ZigbeeDeviceRegistry()

        val unchanged = device("0x001")
        val original = device("0x002")
        val removed = device("0x003")

        registry.reconcile(
            snapshot = listOf(unchanged, original, removed),
        )

        val updated = original.copy(
            friendlyName = "renamed-device",
        )
        val added = device("0x004")

        val snapshot = listOf(unchanged, updated, added)
        val changes = registry.reconcile(
            snapshot = snapshot,
        )

        expectThat(subject = changes) {
            get(ZigbeeDeviceChanges::added)
                .containsExactly(listOf(added))

            get(ZigbeeDeviceChanges::updated)
                .containsExactly(listOf(updated))

            get(ZigbeeDeviceChanges::removed)
                .containsExactly(listOf(removed))

            get(ZigbeeDeviceChanges::isEmpty)
                .isFalse()
        }

        expectThat(subject = registry.findByIeeeAddress("0x001"))
            .isEqualTo(unchanged)

        expectThat(subject = registry.findByIeeeAddress("0x002"))
            .isEqualTo(updated)

        expectThat(subject = registry.findByIeeeAddress("0x003"))
            .isNull()

        expectThat(subject = registry.findByIeeeAddress("0x004"))
            .isEqualTo(added)
    }

    test(name = "Finding a device by its IEEE address should return the device") {
        val registry = ZigbeeDeviceRegistry()
        val device = device("0x001")

        registry.reconcile(listOf(device))

        expectThat(registry.findByIeeeAddress("0x001"))
            .isEqualTo(device)
    }

    test(name = "Finding a device by an unknown IEEE address should return null") {
        val registry = ZigbeeDeviceRegistry()

        registry.reconcile(listOf(device("0x001")))

        expectThat(registry.findByIeeeAddress("0x002"))
            .isNull()
    }

    test(name = "Finding a device by its friendly name should return the device") {
        val registry = ZigbeeDeviceRegistry()
        val device = device(
            ieeeAddress = "0x001",
            friendlyName = "light.kitchen",
        )

        registry.reconcile(listOf(device))

        expectThat(registry.findByFriendlyName("light.kitchen"))
            .isEqualTo(device)
    }

    test(name = "Finding a device by an unknown friendly name should return null") {
        val registry = ZigbeeDeviceRegistry()

        registry.reconcile(listOf(device("0x001")))

        expectThat(registry.findByFriendlyName("unknown"))
            .isNull()
    }

    test(name = "Finding a renamed device should only succeed using its new friendly name") {
        val registry = ZigbeeDeviceRegistry()
        val original = device(
            ieeeAddress = "0x001",
            friendlyName = "light.kitchen",
        )
        val renamed = original.copy(
            friendlyName = "light.kitchen-ceiling",
        )

        registry.reconcile(listOf(original))
        registry.reconcile(listOf(renamed))

        expectThat(registry.findByFriendlyName("light.kitchen"))
            .isNull()

        expectThat(registry.findByFriendlyName("light.kitchen-ceiling"))
            .isEqualTo(renamed)
    }

    test(name = "Finding a removed device should return null") {
        val registry = ZigbeeDeviceRegistry()
        val device = device("0x001")

        registry.reconcile(listOf(device))
        registry.reconcile(emptyList())

        expectThat(registry.findByIeeeAddress(device.ieeeAddress))
            .isNull()

        expectThat(registry.findByFriendlyName(device.friendlyName))
            .isNull()
    }
}

private fun device(
    ieeeAddress: String,
    friendlyName: String = ieeeAddress,
): ZigbeeDevice = ZigbeeDevice(
    ieeeAddress = ieeeAddress,
    friendlyName = friendlyName,
    disabled = false,
    supported = true,
    description = null,
    definition = null,
    interviewState = ZigbeeInterviewState.SUCCESSFUL,
)
