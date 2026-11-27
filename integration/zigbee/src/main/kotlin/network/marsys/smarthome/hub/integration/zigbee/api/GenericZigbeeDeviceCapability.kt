package network.marsys.smarthome.hub.integration.zigbee.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class GenericZigbeeDeviceCapability(
    @SerialName("access")
    val access: Int,
    @SerialName("name")
    val name: String,
    @SerialName("label")
    val label: String? = null,
    @SerialName("property")
    val property: String,
    @SerialName("type")
    override val type: String,
) : ZigbeeDeviceCapability
