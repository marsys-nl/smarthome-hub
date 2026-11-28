package network.marsys.smarthome.hub.integration.zigbee.helper

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.receiveAsFlow
import network.marsys.smarthome.hub.infrastructure.mqtt.ConnectionState
import network.marsys.smarthome.hub.infrastructure.mqtt.Message
import network.marsys.smarthome.hub.infrastructure.mqtt.MqttClient

internal class FakeMqttClient : MqttClient {
    private val messages = Channel<Message>(
        capacity = Channel.UNLIMITED,
    )

    val subscription: Deferred<List<String>>
        field = CompletableDeferred<List<String>>()

    override val state: StateFlow<ConnectionState> =
        MutableStateFlow(ConnectionState.Connected)

    override fun subscribe(vararg filters: String): Flow<Message> =
        flow {
            subscription.complete(filters.toList())
            emitAll(messages.receiveAsFlow())
        }

    override suspend fun publish(topic: String, payload: ByteArray) {
        error("Publishing is not supported by this fake.")
    }

    suspend fun receive(topic: String, payload: String) {
        messages.send(
            Message(
                topic = topic,
                payload = payload.encodeToByteArray(),
            ),
        )
    }

    suspend fun receive(vararg devices: String) {
        receive(
            topic = "zigbee2mqtt/bridge/devices",
            payload = devices.joinToString(
                prefix = "[",
                postfix = "]",
                separator = ",",
            ),
        )
    }
}
