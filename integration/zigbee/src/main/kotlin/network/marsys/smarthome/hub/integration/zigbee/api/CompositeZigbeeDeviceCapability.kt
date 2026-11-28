package network.marsys.smarthome.hub.integration.zigbee.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class CompositeZigbeeDeviceCapability(
    @SerialName("access")
    val access: Int? = null,
    @SerialName("name")
    val name: String? = null,
    @SerialName("label")
    val label: String? = null,
    @SerialName("property")
    val property: String? = null,
    @SerialName("type")
    override val type: String,
    @SerialName("features")
    val features: List<ZigbeeDeviceCapability>,
) : ZigbeeDeviceCapability
