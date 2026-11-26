package network.marsys.smarthome.hub.infrastructure.mqtt.internal

import de.infix.testBalloon.framework.core.testSuite
import dev.nmarsman.expect.api.expectThat
import dev.nmarsman.expect.api.expectThrows
import dev.nmarsman.expect.assertions.hasCause
import dev.nmarsman.expect.assertions.hasMessage
import dev.nmarsman.expect.assertions.isEqualTo
import dev.nmarsman.expect.assertions.single
import network.marsys.smarthome.hub.infrastructure.mqtt.internal.helper.FakeMqttAsyncClient
import network.marsys.smarthome.hub.infrastructure.mqtt.internal.helper.FakeMqttToken
import network.marsys.smarthome.hub.infrastructure.mqtt.internal.helper.withDisconnectedClient
import org.eclipse.paho.mqttv5.client.IMqttToken
import org.eclipse.paho.mqttv5.client.MqttActionListener
import org.eclipse.paho.mqttv5.common.MqttException

val PahoSubscriptionExecutorTest by testSuite(
    name = "Paho subscription executor test",
) {
    test(name = "Subscribing while disconnected throws SessionException") {
        withDisconnectedClient { client ->
            val executor = PahoSubscriptionExecutor(client)
            val filter = TopicFilter("test/#")

            expectThrows<SessionException> { executor.subscribe(filter) }
                .hasMessage("Failed to subscribe to '${filter.value}'.")
                .hasCause<MqttException>()
        }
    }

    test(name = "Subscribing to a topic succeeds") {
        withDisconnectedClient { delegate ->
            val client = object : FakeMqttAsyncClient(delegate) {
                val subscriptions = mutableListOf<String>()

                @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
                override fun subscribe(filter: String, qos: Int, context: Any?, callback: MqttActionListener): IMqttToken =
                    FakeMqttToken().also {
                        subscriptions.add(filter)
                        callback.onSuccess(it)
                    }
            }

            val executor = PahoSubscriptionExecutor(client)
            val filter = TopicFilter("test/#")

            executor.subscribe(filter)

            expectThat(client.subscriptions)
                .single()
                .isEqualTo(filter.value)
        }
    }

    test(name = "Unsubscribing while disconnected throws SessionException") {
        withDisconnectedClient { client ->
            val executor = PahoSubscriptionExecutor(client)
            val filter = TopicFilter("test/#")

            expectThrows<SessionException> { executor.unsubscribe(filter) }
                .hasMessage("Failed to unsubscribe from '${filter.value}'.")
                .hasCause<MqttException>()
        }
    }

    test(name = "Unsubscribing from an active subscription succeeds") {
        withDisconnectedClient { delegate ->
            val client = object : FakeMqttAsyncClient(delegate) {
                val unsubscriptions = mutableListOf<String>()

                @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
                override fun unsubscribe(filter: String, context: Any?, callback: MqttActionListener): IMqttToken =
                    FakeMqttToken().also {
                        unsubscriptions.add(filter)
                        callback.onSuccess(it)
                    }
            }

            val executor = PahoSubscriptionExecutor(client)
            val filter = TopicFilter("test/#")

            executor.unsubscribe(filter)

            expectThat(client.unsubscriptions)
                .single()
                .isEqualTo(filter.value)
        }
    }
}
