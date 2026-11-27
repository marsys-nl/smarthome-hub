package network.marsys.smarthome.hub.integration.zigbee.api

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject

@Serializable(with = ZigbeeDeviceCapabilitySerializer::class)
internal sealed interface ZigbeeDeviceCapability {
    val type: String
}

internal object ZigbeeDeviceCapabilitySerializer : JsonContentPolymorphicSerializer<ZigbeeDeviceCapability>(
    baseClass = ZigbeeDeviceCapability::class,
) {
    override fun selectDeserializer(
        element: JsonElement,
    ): DeserializationStrategy<ZigbeeDeviceCapability> =
        when {
            "features" in element.jsonObject ->
                CompositeZigbeeDeviceCapability.serializer()

            else ->
                GenericZigbeeDeviceCapability.serializer()
        }
}
