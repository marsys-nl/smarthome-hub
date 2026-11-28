package network.marsys.smarthome.hub.integration.zigbee.internal.mapper

import network.marsys.smarthome.domain.capability.OnOff
import network.marsys.smarthome.hub.integration.zigbee.api.GenericZigbeeDeviceCapability
import network.marsys.smarthome.hub.integration.zigbee.api.ZigbeeDeviceState
import network.marsys.smarthome.hub.integration.zigbee.internal.mapper.capability.ZigbeeCapabilityMapper
import network.marsys.smarthome.hub.integration.zigbee.internal.resolve

internal object ZigbeeOnOffMapper : ZigbeeCapabilityMapper<GenericZigbeeDeviceCapability.Binary, OnOff> {
    override fun map(
        capability: GenericZigbeeDeviceCapability.Binary,
        state: ZigbeeDeviceState,
    ): OnOff? =
        capability
            .resolve(state = state)
            ?.let {
                OnOff(current = it)
            }
}
