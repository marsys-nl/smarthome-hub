package network.marsys.smarthome.hub.integration.zigbee.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
internal data class ZigbeeDeviceDefinition(
    @SerialName("model")
    val model: String,
    @SerialName("vendor")
    val vendor: String,
    @SerialName("description")
    val description: String,
    @SerialName("exposes")
    val exposes: List<JsonObject>,
)
