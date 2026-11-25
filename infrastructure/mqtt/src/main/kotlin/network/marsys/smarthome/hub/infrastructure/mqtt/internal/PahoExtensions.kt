package network.marsys.smarthome.hub.infrastructure.mqtt.internal

import kotlinx.coroutines.suspendCancellableCoroutine
import org.eclipse.paho.mqttv5.client.IMqttAsyncClient
import org.eclipse.paho.mqttv5.client.IMqttToken
import org.eclipse.paho.mqttv5.client.MqttActionListener
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal suspend fun await(operation: (MqttActionListener) -> Unit): IMqttToken =
    suspendCancellableCoroutine { continuation ->
        try {
            operation.invoke(
                object : MqttActionListener {
                    override fun onSuccess(asyncActionToken: IMqttToken) {
                        continuation.resume(asyncActionToken)
                    }

                    override fun onFailure(
                        asyncActionToken: IMqttToken,
                        exception: Throwable,
                    ) {
                        continuation.resumeWithException(exception)
                    }
                },
            )
        } catch (exception: Exception) {
            continuation.resumeWithException(exception)
        }
    }

internal suspend fun IMqttAsyncClient.connectAwait(options: MqttConnectionOptions): IMqttToken =
    await { listener ->
        connect(options, null, listener)
    }

internal suspend fun IMqttAsyncClient.disconnectAwait(): IMqttToken =
    await { listener ->
        disconnect(null, listener)
    }

internal suspend fun IMqttAsyncClient.subscribeAwait(filter: String, qos: Int): IMqttToken =
    await { listener ->
        subscribe(filter, qos, null, listener)
    }

internal suspend fun IMqttAsyncClient.unsubscribeAwait(filter: String): IMqttToken =
    await { listener ->
        unsubscribe(filter, null, listener)
    }

internal suspend fun IMqttAsyncClient.publishAwait(
    topic: String,
    payload: ByteArray,
    qos: Int,
    retained: Boolean,
): IMqttToken = await { listener ->
    publish(topic, payload, qos, retained, null, listener)
}
