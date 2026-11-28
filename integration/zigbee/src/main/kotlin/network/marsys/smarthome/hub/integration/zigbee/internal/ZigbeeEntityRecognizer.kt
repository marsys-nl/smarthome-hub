package network.marsys.smarthome.hub.integration.zigbee.internal

import network.marsys.smarthome.domain.entity.Entity
import network.marsys.smarthome.domain.entity.Light
import network.marsys.smarthome.hub.integration.zigbee.api.ZigbeeDevice
import network.marsys.smarthome.hub.integration.zigbee.api.ZigbeeInterviewState

internal class ZigbeeEntityRecognizer {
    fun recognize(device: ZigbeeDevice): Entity.Type<*>? {
        if (device.disabled || !device.supported || device.interviewState != ZigbeeInterviewState.SUCCESSFUL) {
            return null
        }

        val definition = device.definition
            ?: return null

        return when {
            definition.exposes.any { it.type == "light" } -> Light
            else -> null
        }
    }
}
