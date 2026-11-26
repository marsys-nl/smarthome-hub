package network.marsys.smarthome.hub.infrastructure.mqtt

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import network.marsys.smarthome.hub.infrastructure.mqtt.internal.PahoMqttClient
import org.eclipse.paho.mqttv5.client.MqttAsyncClient
import org.eclipse.paho.mqttv5.client.MqttConnectionOptionsBuilder
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence

private const val MQTT_CLIENT_POSTFIX_LENGTH = 8

private val characters: List<Char> = ('a'..'z') + ('0'..'9')
private val postfix: String = List(MQTT_CLIENT_POSTFIX_LENGTH) { characters.random() }
    .joinToString("")

@Suppress("FunctionName")
fun MqttClient(configuration: MqttClientConfiguration): ManagedMqttClient {
    val client = MqttAsyncClient(
        configuration.broker,
        configuration.client + "-" + postfix,
        MemoryPersistence(),
    )

    val options = MqttConnectionOptionsBuilder()
        .cleanStart(true)
        .automaticReconnect(false)
        .username(configuration.username)
        .password(configuration.password?.encodeToByteArray())
        .build()

    return PahoMqttClient(
        client = client,
        options = options,
    )
}

interface MqttClient {
    val state: StateFlow<ConnectionState>

    suspend fun publish(topic: String, payload: ByteArray)

    fun subscribe(vararg filters: String): Flow<Message>
}
