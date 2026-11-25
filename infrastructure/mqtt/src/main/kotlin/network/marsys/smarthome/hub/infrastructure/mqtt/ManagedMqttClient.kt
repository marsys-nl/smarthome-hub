package network.marsys.smarthome.hub.infrastructure.mqtt

interface ManagedMqttClient : MqttClient {
    suspend fun close()
}
