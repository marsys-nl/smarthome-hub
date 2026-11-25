package network.marsys.smarthome.hub.infrastructure.mqtt.internal

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.eclipse.paho.mqttv5.client.IMqttAsyncClient
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions
import org.eclipse.paho.mqttv5.common.MqttException

internal class PahoConnection(
    private val client: IMqttAsyncClient,
    private val options: MqttConnectionOptions,
) : Connection {
    private val lock = Any()

    override val connected: StateFlow<Boolean>
        field = MutableStateFlow(false)

    override suspend fun connect() {
        try {
            client.connectAwait(options)
        } catch (exception: MqttException) {
            throw ConnectionException(
                message = "Failed to connect to MQTT broker at ${client.serverURI}.",
                cause = exception,
            )
        } finally {
            updateConnectionState()
        }
    }

    override suspend fun disconnect() {
        try {
            client.disconnectAwait()
        } catch (exception: MqttException) {
            throw ConnectionException(
                message = "Failed to disconnect from MQTT broker at ${client.serverURI}.",
                cause = exception,
            )
        } finally {
            updateConnectionState()
        }
    }

    fun updateConnectionState() {
        synchronized(lock) {
            connected.value = client.isConnected
        }
    }
}
