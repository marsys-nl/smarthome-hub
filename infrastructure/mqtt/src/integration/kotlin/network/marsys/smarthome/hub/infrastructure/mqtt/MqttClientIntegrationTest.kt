package network.marsys.smarthome.hub.infrastructure.mqtt

import de.infix.testBalloon.framework.core.testSuite
import dev.nmarsman.expect.api.expectThat
import dev.nmarsman.expect.api.expectThrows
import dev.nmarsman.expect.assertions.isEqualTo
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withTimeout
import network.marsys.smarthome.hub.infrastructure.mqtt.helper.use
import network.marsys.smarthome.hub.infrastructure.mqtt.helper.withBroker
import network.marsys.smarthome.hub.infrastructure.mqtt.internal.ClientClosedException
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

val MqttClientIntegrationTest by testSuite(
    name = "MQTT client integration tests",
) {
    test(name = "Creating client does not establish a connection") {
        runBlocking {
            withBroker { broker ->
                MqttClient(
                    configuration = MqttClientConfiguration(
                        broker = broker.uri,
                        client = UUID.randomUUID().toString(),
                    ),
                ).use { client ->
                    expectThat(client.state.value)
                        .isEqualTo(ConnectionState.Disconnected)
                }
            }
        }
    }

    test(name = "Connects when subscription is collected") {
        runBlocking {
            withBroker { broker ->
                MqttClient(
                    MqttClientConfiguration(
                        broker = broker.uri,
                        client = UUID.randomUUID().toString(),
                    ),
                ).use { client ->
                    val collector = launch {
                        client.subscribe("home/#")
                            .collect()
                    }

                    try {
                        withTimeout(10.seconds) {
                            client.state.first {
                                it == ConnectionState.Connected
                            }
                        }

                        expectThat(client.state.value)
                            .isEqualTo(ConnectionState.Connected)
                    } finally {
                        collector.cancelAndJoin()
                    }
                }
            }
        }
    }

    test(name = "Disconnects after last subscription completes") {
        runBlocking {
            withBroker { broker ->
                MqttClient(
                    MqttClientConfiguration(
                        broker = broker.uri,
                        client = UUID.randomUUID().toString(),
                    ),
                ).use { client ->
                    val collector = launch {
                        client.subscribe("home/#")
                            .collect()
                    }

                    try {
                        withTimeout(10.seconds) {
                            client.state.first {
                                it == ConnectionState.Connected
                            }
                        }
                    } finally {
                        collector.cancelAndJoin()
                    }

                    withTimeout(15.seconds) {
                        client.state.first {
                            it == ConnectionState.Disconnected
                        }
                    }

                    expectThat(client.state.value)
                        .isEqualTo(ConnectionState.Disconnected)
                }
            }
        }
    }

    test(name = "Closes active subscription") {
        runBlocking {
            supervisorScope {
                withBroker { broker ->
                    MqttClient(
                        MqttClientConfiguration(
                            broker = broker.uri,
                            client = UUID.randomUUID().toString(),
                        ),
                    ).use { client ->
                        val collector = async {
                            client.subscribe("home/#")
                                .collect()
                        }

                        withTimeout(10.seconds) {
                            client.state.first {
                                it == ConnectionState.Connected
                            }
                        }

                        client.close()

                        expectThrows<ClientClosedException> {
                            withTimeout(5.seconds) {
                                collector.await()
                            }
                        }

                        expectThat(client.state.value)
                            .isEqualTo(ConnectionState.Disconnected)
                    }
                }
            }
        }
    }
}
