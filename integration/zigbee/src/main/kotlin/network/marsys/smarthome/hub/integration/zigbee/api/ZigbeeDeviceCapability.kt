package network.marsys.smarthome.hub.integration.zigbee.api

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
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
    ): DeserializationStrategy<ZigbeeDeviceCapability> {
        val content = element.jsonObject

        return when {
            "features" in content ->
                CompositeZigbeeDeviceCapability.serializer()

            content["type"] == JsonPrimitive("binary") ->
                GenericZigbeeDeviceCapability.Binary.serializer()

            content["type"] == JsonPrimitive("numeric") ->
                GenericZigbeeDeviceCapability.Numeric.serializer()

            else ->
                GenericZigbeeDeviceCapability.Default.serializer()
        }
    }
}
