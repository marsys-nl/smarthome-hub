package network.marsys.smarthome.hub.integration.zigbee.helper

import kotlinx.serialization.json.JsonPrimitive
import network.marsys.smarthome.domain.entity.EntityIdentifier
import network.marsys.smarthome.domain.entity.Light
import network.marsys.smarthome.domain.entity.naming
import network.marsys.smarthome.hub.integration.zigbee.api.CompositeZigbeeDeviceCapability
import network.marsys.smarthome.hub.integration.zigbee.api.GenericZigbeeDeviceCapability
import network.marsys.smarthome.hub.integration.zigbee.api.ZigbeeDevice
import network.marsys.smarthome.hub.integration.zigbee.api.ZigbeeDeviceCapability
import network.marsys.smarthome.hub.integration.zigbee.api.ZigbeeDeviceDefinition
import network.marsys.smarthome.hub.integration.zigbee.api.ZigbeeInterviewState

internal val power = GenericZigbeeDeviceCapability.Numeric(
    access = 5,
    name = "power",
    property = "power",
)

internal val linkQuality = GenericZigbeeDeviceCapability.Numeric(
    access = 1,
    name = "linkquality",
    property = "linkquality",
)

internal val onOff = GenericZigbeeDeviceCapability.Binary(
    access = 7,
    name = "state",
    property = "state",
    valueOn = JsonPrimitive("ON"),
    valueOff = JsonPrimitive("OFF"),
    valueToggle = JsonPrimitive("TOGGLE"),
)

internal val brightness = GenericZigbeeDeviceCapability.Numeric(
    access = 7,
    name = "brightness",
    property = "brightness",
    valueMin = 0.0,
    valueMax = 254.0,
)

internal val light = CompositeZigbeeDeviceCapability(
    type = "light",
    features = listOf(onOff, brightness),
)

internal fun device(
    ieeeAddress: String = "0x943469fffeb3c843",
    friendlyName: String = "test-device",
    exposes: List<ZigbeeDeviceCapability> = emptyList(),
): ZigbeeDevice = ZigbeeDevice(
    ieeeAddress = ieeeAddress,
    friendlyName = friendlyName,
    disabled = false,
    supported = true,
    description = null,
    definition = ZigbeeDeviceDefinition(
        model = "TEST",
        vendor = "Test vendor",
        description = "Test device",
        exposes = exposes,
    ),
    interviewState = ZigbeeInterviewState.SUCCESSFUL,
)

internal fun light(
    features: List<ZigbeeDeviceCapability> =
        listOf(onOff, brightness),
    exposes: List<ZigbeeDeviceCapability> =
        listOf(
            CompositeZigbeeDeviceCapability(
                type = "light",
                features = features,
            ),
        ),
): ZigbeeDevice = device(
    exposes = exposes,
)

internal fun entity(
    ieeeAddress: String = "0x943469fffeb3c843",
    state: Light.State = Light.State.Unknown,
): Light = Light(
    identifier = EntityIdentifier("zigbee.$ieeeAddress"),
    naming = Light.naming(),
    state = state,
)
