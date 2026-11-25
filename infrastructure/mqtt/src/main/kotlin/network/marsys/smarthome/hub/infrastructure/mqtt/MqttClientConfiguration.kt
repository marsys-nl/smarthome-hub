package network.marsys.smarthome.hub.infrastructure.mqtt

data class MqttClientConfiguration(
    val broker: String,
    val client: String,
    val username: String? = null,
    val password: String? = null,
)
