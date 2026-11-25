package network.marsys.smarthome.hub.infrastructure.mqtt.internal

import org.eclipse.paho.mqttv5.client.IMqttAsyncClient
import org.eclipse.paho.mqttv5.common.MqttException

internal class PahoPublishExecutor(
    private val client: IMqttAsyncClient,
) : PublishExecutor {
    override suspend fun publish(
        topic: TopicName,
        payload: ByteArray,
    ) {
        try {
            client.publishAwait(
                topic = topic.value,
                payload = payload,
                qos = 0,
                retained = false,
            )
        } catch (exception: MqttException) {
            throw PublishException(
                message = "Failed to publish message to '${topic.value}'.",
                cause = exception,
            )
        }
    }
}
