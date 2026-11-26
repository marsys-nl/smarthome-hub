package network.marsys.smarthome.hub.infrastructure.mqtt.internal.helper

import network.marsys.smarthome.hub.infrastructure.mqtt.Message
import org.eclipse.paho.mqttv5.client.IMqttAsyncClient
import org.eclipse.paho.mqttv5.client.IMqttToken
import org.eclipse.paho.mqttv5.client.MqttActionListener
import org.eclipse.paho.mqttv5.client.MqttAsyncClient
import org.eclipse.paho.mqttv5.client.MqttCallback
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence
import java.util.UUID

internal open class FakeMqttAsyncClient(
    delegate: IMqttAsyncClient,
) : IMqttAsyncClient by delegate

internal open class ConnectableFakeMqttAsyncClient(
    delegate: IMqttAsyncClient,
) : FakeMqttAsyncClient(delegate) {
    private var connected: Boolean = false
    var connections: Int = 0
        private set

    var disconnects: Int = 0
        private set

    val messages: List<Message>
        field = mutableListOf<Message>()

    val subscriptions: List<String>
        field = mutableListOf<String>()

    val unsubscriptions: List<String>
        field = mutableListOf<String>()

    lateinit var callback: MqttCallback
        private set

    override fun isConnected(): Boolean = connected

    @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
    override fun setCallback(callback: MqttCallback) {
        this.callback = callback
    }

    @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
    override fun connect(
        options: MqttConnectionOptions,
        context: Any?,
        callback: MqttActionListener,
    ): IMqttToken = FakeMqttToken().also {
        connected = true
        connections++
        callback.onSuccess(it)
    }

    @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
    override fun disconnect(
        context: Any?,
        callback: MqttActionListener,
    ): IMqttToken = FakeMqttToken().also {
        disconnects++
        connected = false
        callback.onSuccess(it)
    }

    @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
    override fun publish(
        topic: String,
        payload: ByteArray,
        qos: Int,
        retained: Boolean,
        context: Any?,
        callback: MqttActionListener,
    ): IMqttToken = FakeMqttToken().also {
        messages.add(
            Message(
                topic = topic,
                payload = payload.copyOf(),
            ),
        )

        callback.onSuccess(it)
    }

    @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
    override fun subscribe(filter: String, qos: Int, context: Any?, callback: MqttActionListener): IMqttToken =
        FakeMqttToken().also {
            subscriptions.add(filter)
            callback.onSuccess(it)
        }

    @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
    override fun unsubscribe(filter: String, context: Any?, callback: MqttActionListener): IMqttToken =
        FakeMqttToken().also {
            unsubscriptions.add(filter)
            callback.onSuccess(it)
        }
}

internal suspend fun withDisconnectedClient(
    block: suspend (IMqttAsyncClient) -> Unit,
) {
    val client = MqttAsyncClient(
        "tcp://localhost:1883",
        UUID.randomUUID().toString(),
        MemoryPersistence(),
    )

    try {
        block(client)
    } finally {
        client.close()
    }
}
