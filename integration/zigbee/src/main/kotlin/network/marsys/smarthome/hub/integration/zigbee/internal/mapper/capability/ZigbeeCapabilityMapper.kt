package network.marsys.smarthome.hub.integration.zigbee.internal.mapper.capability

import network.marsys.smarthome.domain.capability.Capability
import network.marsys.smarthome.hub.integration.zigbee.api.ZigbeeDeviceCapability
import network.marsys.smarthome.hub.integration.zigbee.api.ZigbeeDeviceState

internal interface ZigbeeCapabilityMapper<Z : ZigbeeDeviceCapability, C : Capability<*, *>> {
    fun map(capability: Z, state: ZigbeeDeviceState): C?
}
