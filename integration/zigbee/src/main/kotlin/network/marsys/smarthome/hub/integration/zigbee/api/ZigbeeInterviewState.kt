package network.marsys.smarthome.hub.integration.zigbee.api

import kotlinx.serialization.Serializable

@Serializable
internal enum class ZigbeeInterviewState {
    PENDING,
    IN_PROGRESS,
    SUCCESSFUL,
    FAILED,
}
