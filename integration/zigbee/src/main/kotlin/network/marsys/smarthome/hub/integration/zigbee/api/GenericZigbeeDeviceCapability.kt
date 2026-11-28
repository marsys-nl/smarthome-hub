package network.marsys.smarthome.hub.integration.zigbee.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive

@Serializable
internal sealed interface GenericZigbeeDeviceCapability : ZigbeeDeviceCapability {
    @SerialName("access")
    val access: Int

    @SerialName("name")
    val name: String

    @SerialName("label")
    val label: String?

    @SerialName("property")
    val property: String

    @Serializable
    data class Binary(
        @SerialName("access")
        override val access: Int,
        @SerialName("name")
        override val name: String,
        @SerialName("label")
        override val label: String? = null,
        @SerialName("property")
        override val property: String,
        @SerialName("value_on")
        val valueOn: JsonPrimitive = JsonPrimitive(true),
        @SerialName("value_off")
        val valueOff: JsonPrimitive = JsonPrimitive(false),
        @SerialName("value_toggle")
        val valueToggle: JsonPrimitive? = null,
    ) : GenericZigbeeDeviceCapability {
        override val type: String
            get() = "binary"
    }

    @Serializable
    data class Numeric(
        @SerialName("access")
        override val access: Int,
        @SerialName("name")
        override val name: String,
        @SerialName("label")
        override val label: String? = null,
        @SerialName("property")
        override val property: String,
        @SerialName("value_min")
        val valueMin: Double? = null,
        @SerialName("value_max")
        val valueMax: Double? = null,
        @SerialName("value_step")
        val valueStep: Double? = null,
        @SerialName("unit")
        val unit: String? = null,
    ) : GenericZigbeeDeviceCapability {
        override val type: String
            get() = "numeric"
    }

    @Serializable
    data class Default(
        @SerialName("access")
        override val access: Int,
        @SerialName("name")
        override val name: String,
        @SerialName("label")
        override val label: String? = null,
        @SerialName("property")
        override val property: String,
        @SerialName("type")
        override val type: String,
    ) : GenericZigbeeDeviceCapability
}
