package network.marsys.smarthome.hub.integration.zigbee.internal.mapper.state

import network.marsys.smarthome.domain.entity.Entity
import network.marsys.smarthome.hub.integration.zigbee.api.ZigbeeDevice
import network.marsys.smarthome.hub.integration.zigbee.api.ZigbeeDeviceState

internal interface ZigbeeEntityStateMapper<S : Entity.State> {
    fun map(
        device: ZigbeeDevice,
        state: ZigbeeDeviceState,
    ): S?
}
