package network.marsys.smarthome.hub.infrastructure.mqtt.internal

import network.marsys.smarthome.hub.infrastructure.mqtt.internal.SubscriptionAcknowledgement.validateSubscribeAcknowledgement
import network.marsys.smarthome.hub.infrastructure.mqtt.internal.SubscriptionAcknowledgement.validateUnsubscribeAcknowledgement
import org.eclipse.paho.mqttv5.client.IMqttAsyncClient
import org.eclipse.paho.mqttv5.common.MqttException

internal class PahoSubscriptionExecutor(
    private val client: IMqttAsyncClient,
) : SubscriptionExecutor {
    override suspend fun subscribe(filter: TopicFilter) {
        val token = try {
            client.subscribeAwait(
                filter = filter.value,
                qos = 0,
            )
        } catch (exception: MqttException) {
            throw SessionException(
                message = "Failed to subscribe to '${filter.value}'.",
                cause = exception,
            )
        }

        validateSubscribeAcknowledgement(
            filter = filter,
            reasons = token.reasonCodes,
        )
    }

    override suspend fun unsubscribe(filter: TopicFilter) {
        val token = try {
            client.unsubscribeAwait(
                filter = filter.value,
            )
        } catch (exception: MqttException) {
            throw SessionException(
                message = "Failed to unsubscribe from '${filter.value}'.",
                cause = exception,
            )
        }

        validateUnsubscribeAcknowledgement(
            filter = filter,
            reasons = token.reasonCodes,
        )
    }
}
