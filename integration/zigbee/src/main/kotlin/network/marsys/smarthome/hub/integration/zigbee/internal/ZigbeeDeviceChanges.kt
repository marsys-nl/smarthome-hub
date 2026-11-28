package network.marsys.smarthome.hub.integration.zigbee.internal

import network.marsys.smarthome.hub.integration.zigbee.api.ZigbeeDevice

internal data class ZigbeeDeviceChanges(
    val added: List<ZigbeeDevice>,
    val updated: List<ZigbeeDevice>,
    val removed: List<ZigbeeDevice>,
) {
    val isEmpty: Boolean
        get() = added.isEmpty() &&
            updated.isEmpty() &&
            removed.isEmpty()
}
