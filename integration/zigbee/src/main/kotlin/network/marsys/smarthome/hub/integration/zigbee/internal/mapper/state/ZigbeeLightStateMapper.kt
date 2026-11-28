package network.marsys.smarthome.hub.integration.zigbee.internal.mapper.state

import network.marsys.smarthome.domain.capability.Capability.Companion.optional
import network.marsys.smarthome.domain.capability.Capability.Companion.required
import network.marsys.smarthome.domain.entity.Light
import network.marsys.smarthome.hub.integration.zigbee.api.CompositeZigbeeDeviceCapability
import network.marsys.smarthome.hub.integration.zigbee.api.ZigbeeDevice
import network.marsys.smarthome.hub.integration.zigbee.api.ZigbeeDeviceState
import network.marsys.smarthome.hub.integration.zigbee.internal.feature
import network.marsys.smarthome.hub.integration.zigbee.internal.mapper.ZigbeeBrightnessMapper
import network.marsys.smarthome.hub.integration.zigbee.internal.mapper.ZigbeeOnOffMapper

internal object ZigbeeLightStateMapper : ZigbeeEntityStateMapper<Light.State.Known> {
    override fun map(device: ZigbeeDevice, state: ZigbeeDeviceState): Light.State.Known? {
        val definition = device.definition ?: return null

        val light = definition.exposes
            .filterIsInstance<CompositeZigbeeDeviceCapability>()
            .singleOrNull { it.type == "light" }
            ?: return null

        val onOff = light.feature(
            mapper = ZigbeeOnOffMapper,
            state = state,
            predicate = { it.name == "state" },
        ) ?: return null

        val brightness = light.feature(
            mapper = ZigbeeBrightnessMapper,
            state = state,
            predicate = { it.name == "brightness" },
        )

        return Light.State.Known(
            onOff = required(onOff),
            brightness = optional(brightness),
        )
    }
}
