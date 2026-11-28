package network.marsys.smarthome.hub.integration.zigbee.internal

import network.marsys.smarthome.hub.integration.zigbee.api.ZigbeeDevice

internal class ZigbeeDeviceRegistry {
    private val devices = mutableMapOf<String, ZigbeeDevice>()

    fun reconcile(snapshot: List<ZigbeeDevice>): ZigbeeDeviceChanges {
        val current = snapshot.associateBy { it.ieeeAddress }

        val added = current.values.filter { device ->
            device.ieeeAddress !in devices
        }

        val updated = current.values.filter { device ->
            val existing = devices[device.ieeeAddress]
            existing != null && existing != device
        }

        val removed = devices.values.filter { device ->
            device.ieeeAddress !in current
        }

        removed.forEach {
            devices.remove(it.ieeeAddress)
        }

        added.forEach {
            devices[it.ieeeAddress] = it
        }

        updated.forEach {
            devices[it.ieeeAddress] = it
        }

        return ZigbeeDeviceChanges(
            added = added,
            updated = updated,
            removed = removed,
        )
    }

    fun findByIeeeAddress(ieeeAddress: String): ZigbeeDevice? =
        devices[ieeeAddress]

    fun findByFriendlyName(friendlyName: String): ZigbeeDevice? =
        devices.values.firstOrNull {
            it.friendlyName == friendlyName
        }
}
