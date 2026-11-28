package network.marsys.smarthome.hub.integration.zigbee.internal.mapper

import network.marsys.smarthome.domain.capability.Brightness
import network.marsys.smarthome.domain.unit.percent
import network.marsys.smarthome.hub.integration.zigbee.api.GenericZigbeeDeviceCapability
import network.marsys.smarthome.hub.integration.zigbee.api.ZigbeeDeviceState
import network.marsys.smarthome.hub.integration.zigbee.internal.mapper.capability.ZigbeeCapabilityMapper
import network.marsys.smarthome.hub.integration.zigbee.internal.normalize
import network.marsys.smarthome.hub.integration.zigbee.internal.resolve

internal object ZigbeeBrightnessMapper : ZigbeeCapabilityMapper<GenericZigbeeDeviceCapability.Numeric, Brightness> {
    override fun map(
        capability: GenericZigbeeDeviceCapability.Numeric,
        state: ZigbeeDeviceState,
    ): Brightness? {
        val value = capability.resolve(state)
            ?: return null

        val normalized = capability.normalize(value)
            ?: return null

        return Brightness(current = (normalized * 100.0).percent)
    }
}
