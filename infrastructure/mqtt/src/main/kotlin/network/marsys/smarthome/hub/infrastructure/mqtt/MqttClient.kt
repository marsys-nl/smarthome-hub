package network.marsys.smarthome.hub.infrastructure.mqtt

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import network.marsys.smarthome.hub.infrastructure.mqtt.internal.PahoMqttClient
import org.eclipse.paho.mqttv5.client.MqttAsyncClient
import org.eclipse.paho.mqttv5.client.MqttConnectionOptionsBuilder
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence

@Suppress("FunctionName")
fun MqttClient(configuration: MqttClientConfiguration): ManagedMqttClient {
    val client = MqttAsyncClient(
        configuration.broker,
        "",
        MemoryPersistence(),
    )

    val options = MqttConnectionOptionsBuilder()
        .cleanStart(true)
        .automaticReconnect(false)
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
