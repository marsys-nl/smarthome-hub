package network.marsys.smarthome.hub.integration.zigbee.internal

import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import network.marsys.smarthome.domain.capability.Capability
import network.marsys.smarthome.hub.integration.zigbee.api.CompositeZigbeeDeviceCapability
import network.marsys.smarthome.hub.integration.zigbee.api.GenericZigbeeDeviceCapability
import network.marsys.smarthome.hub.integration.zigbee.api.ZigbeeDeviceCapability
import network.marsys.smarthome.hub.integration.zigbee.api.ZigbeeDeviceState
import network.marsys.smarthome.hub.integration.zigbee.internal.mapper.capability.ZigbeeCapabilityMapper

internal fun GenericZigbeeDeviceCapability.Binary.resolve(
    state: ZigbeeDeviceState,
): Boolean? =
    when (state[property]) {
        valueOn -> true
        valueOff -> false
        else -> null
    }

internal fun GenericZigbeeDeviceCapability.Numeric.resolve(
    state: ZigbeeDeviceState,
): Double? {
    val value = state[property] as? JsonPrimitive
        ?: return null

    if (value.isString) {
        return null
    }

    return value.doubleOrNull
        ?.takeIf { it.isFinite() }
}

internal fun GenericZigbeeDeviceCapability.Numeric.normalize(
    value: Double,
): Double? {
    val min = valueMin ?: return null
    val max = valueMax ?: return null

    if (!min.isFinite() || !max.isFinite() || !value.isFinite()) {
        return null
    }

    if (max <= min || value !in min..max) {
        return null
    }

    val range = max - min

    if (!range.isFinite()) {
        return null
    }

    return (value - min) / range
}

internal inline fun <reified C : ZigbeeDeviceCapability> CompositeZigbeeDeviceCapability.find(
    predicate: (C) -> Boolean,
): C? = features
    .filterIsInstance<C>()
    .singleOrNull(predicate)

internal inline fun <reified Z : ZigbeeDeviceCapability, C : Capability<*, *>> CompositeZigbeeDeviceCapability.feature(
    mapper: ZigbeeCapabilityMapper<Z, C>,
    state: ZigbeeDeviceState,
    predicate: (Z) -> Boolean,
): C? = find(predicate)
    ?.let {
        mapper.map(it, state)
    }
