package network.marsys.smarthome.hub.infrastructure.mqtt.helper

import network.marsys.smarthome.hub.infrastructure.mqtt.ManagedMqttClient

internal suspend fun <T> ManagedMqttClient.use(
    block: suspend (ManagedMqttClient) -> T,
): T {
    try {
        return block.invoke(this)
    } finally {
        close()
    }
}
