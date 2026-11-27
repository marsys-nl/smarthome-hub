package network.marsys.smarthome.hub.integration.zigbee.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class ZigbeeDevice(
    @SerialName("ieee_address")
    val ieeeAddress: String,
    @SerialName("friendly_name")
    val friendlyName: String,
    @SerialName("disabled")
    val disabled: Boolean,
    @SerialName("supported")
    val supported: Boolean,
    @SerialName("description")
    val description: String? = null,
    @SerialName("definition")
    val definition: ZigbeeDeviceDefinition? = null,
    @SerialName("interview_state")
    val interviewState: ZigbeeInterviewState,
)
