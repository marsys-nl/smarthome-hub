package network.marsys.smarthome.hub.infrastructure.mqtt.internal

import de.infix.testBalloon.framework.core.testSuite
import dev.nmarsman.expect.api.expectThat
import dev.nmarsman.expect.api.expectThrows
import dev.nmarsman.expect.assertions.isA
import dev.nmarsman.expect.assertions.isEqualTo
import dev.nmarsman.expect.assertions.isTrue
import dev.nmarsman.expect.assertions.single
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import network.marsys.smarthome.hub.infrastructure.mqtt.ConnectionState
import network.marsys.smarthome.hub.infrastructure.mqtt.Message
import network.marsys.smarthome.hub.infrastructure.mqtt.internal.helper.ConnectableFakeMqttAsyncClient
import network.marsys.smarthome.hub.infrastructure.mqtt.internal.helper.FakeMqttAsyncClient
import network.marsys.smarthome.hub.infrastructure.mqtt.internal.helper.withDisconnectedClient
import org.eclipse.paho.mqttv5.client.MqttCallback
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions
import org.eclipse.paho.mqttv5.common.MqttMessage
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
val PahoMqttClientTest by testSuite(
    name = "Paho MQTT client test",
) {
    test(name = "Client is initially disconnected") {
        runTest {
            withDisconnectedClient { delegate ->
                val client = PahoMqttClient(
                    client = delegate,
                    options = MqttConnectionOptions(),
                    dispatcher = StandardTestDispatcher(testScheduler),
                )

                try {
                    expectThat(client.state.value)
                        .isEqualTo(ConnectionState.Disconnected)
                } finally {
                    client.close()
                }
            }
        }
    }

    test(name = "Callback is registered upon initialization") {
        runTest {
            withDisconnectedClient { delegate ->
                val client = object : FakeMqttAsyncClient(delegate) {
                    val callbacks = mutableListOf<MqttCallback>()

                    @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
                    override fun setCallback(callback: MqttCallback) {
                        callbacks.add(callback)
                    }
                }

                val mqtt = PahoMqttClient(
                    client = client,
                    options = MqttConnectionOptions(),
                    dispatcher = StandardTestDispatcher(testScheduler),
                )

                try {
                    expectThat(client.callbacks)
                        .single()
                        .isA<PahoCallback>()
                } finally {
                    mqtt.close()
                }
            }
        }
    }

    test(name = "Closing the client multiple times closes Paho only once") {
        runTest {
            withDisconnectedClient { delegate ->
                val client = object : FakeMqttAsyncClient(delegate) {
                    val invocations = mutableListOf<Boolean>()

                    @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
                    override fun close(force: Boolean) {
                        invocations.add(force)
                    }
                }

                val mqtt = PahoMqttClient(
                    client = client,
                    options = MqttConnectionOptions(),
                    dispatcher = StandardTestDispatcher(testScheduler),
                )

                mqtt.close()
                mqtt.close()

                expectThat(client.invocations)
                    .single()
                    .isTrue()
            }
        }
    }

    test(name = "Concurrent close operations close Paho only once") {
        runTest {
            withDisconnectedClient { delegate ->
                val client = object : FakeMqttAsyncClient(delegate) {
                    val invocations = mutableListOf<Boolean>()

                    @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
                    override fun close(force: Boolean) {
                        invocations.add(force)
                    }
                }

                val mqttClient = PahoMqttClient(
                    client = client,
                    options = MqttConnectionOptions(),
                    dispatcher = StandardTestDispatcher(testScheduler),
                )

                List(10) {
                    async {
                        mqttClient.close()
                    }
                }.awaitAll()

                expectThat(client.invocations)
                    .single()
                    .isTrue()
            }
        }
    }

    test(name = "Subscribing after closing the client throws ClientClosedException") {
        runTest {
            withDisconnectedClient { delegate ->
                val client = PahoMqttClient(
                    client = delegate,
                    options = MqttConnectionOptions(),
                    dispatcher = StandardTestDispatcher(testScheduler),
                )

                client.close()

                expectThrows<ClientClosedException> {
                    client.subscribe("test/#").first()
                }
            }
        }
    }

    test(name = "Publishing a message establishes a connection") {
        runTest {
            withDisconnectedClient { delegate ->
                val payload = "Hello world".encodeToByteArray()

                val client = ConnectableFakeMqttAsyncClient(
                    delegate = delegate,
                )

                val mqtt = PahoMqttClient(
                    client = client,
                    options = MqttConnectionOptions(),
                    dispatcher = StandardTestDispatcher(testScheduler),
                )

                withTimeout(5.seconds) {
                    mqtt.publish(
                        topic = "test/topic",
                        payload = payload,
                    )
                }

                expectThat(client.connections)
                    .isEqualTo(1)

                expectThat(client.messages)
                    .single()
                    .isEqualTo(
                        expected = Message(
                            topic = "test/topic",
                            payload = payload,
                        ),
                    )

                expectThat(mqtt.state.value)
                    .isEqualTo(ConnectionState.Connected)
            }
        }
    }

    test(name = "Subscribed messages are delivered to the collector") {
        runTest {
            withDisconnectedClient { delegate ->
                val client = ConnectableFakeMqttAsyncClient(
                    delegate = delegate,
                )

                val mqtt = PahoMqttClient(
                    client = client,
                    options = MqttConnectionOptions(),
                    dispatcher = StandardTestDispatcher(testScheduler),
                )

                val topic = "test/topic"
                val payload = "Hello world".encodeToByteArray()

                val received = async {
                    mqtt.subscribe("test/#").first()
                }

                runCurrent()

                expectThat(mqtt.state.value)
                    .isEqualTo(ConnectionState.Connected)

                expectThat(client.subscriptions)
                    .single()
                    .isEqualTo("test/#")

                client.callback.messageArrived(
                    topic,
                    MqttMessage(payload),
                )

                val message = withTimeout(5.seconds) {
                    received.await()
                }

                expectThat(message)
                    .isEqualTo(
                        expected = Message(
                            topic = "test/topic",
                            payload = payload,
                        ),
                    )
            }
        }
    }
}
