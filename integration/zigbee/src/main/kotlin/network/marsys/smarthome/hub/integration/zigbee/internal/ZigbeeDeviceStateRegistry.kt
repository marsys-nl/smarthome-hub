package network.marsys.smarthome.hub.integration.zigbee.internal

import network.marsys.smarthome.hub.integration.zigbee.api.ZigbeeDeviceState

internal class ZigbeeDeviceStateRegistry {
    private val states = mutableMapOf<String, ZigbeeDeviceState>()

    fun update(ieeeAddress: String, state: ZigbeeDeviceState): ZigbeeDeviceState {
        val current = states[ieeeAddress].orEmpty()

        if (state.isEmpty()) {
            return current
        }

        return (current + state).also { updated ->
            states[ieeeAddress] = updated
        }
    }

    fun findByIeeeAddress(ieeeAddress: String): ZigbeeDeviceState? =
        states[ieeeAddress]

    fun remove(ieeeAddress: String) {
        states.remove(ieeeAddress)
    }

    fun clear() {
        states.clear()
    }
}
