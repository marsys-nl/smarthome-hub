package network.marsys.smarthome.hub.infrastructure.mqtt.internal

import de.infix.testBalloon.framework.core.testSuite
import dev.nmarsman.expect.api.expectThat
import dev.nmarsman.expect.api.expectThrows
import dev.nmarsman.expect.assertions.hasMessage
import dev.nmarsman.expect.assertions.hasSize
import dev.nmarsman.expect.assertions.isEmpty
import dev.nmarsman.expect.assertions.isEqualTo
import dev.nmarsman.expect.assertions.isFalse
import dev.nmarsman.expect.assertions.isTrue
import dev.nmarsman.expect.assertions.single
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import network.marsys.smarthome.hub.infrastructure.mqtt.ConnectionState
import network.marsys.smarthome.hub.infrastructure.mqtt.Message
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
val PublisherTest by testSuite(
    name = "Publisher tests",
) {
    testSuite(
        name = "Configuration",
    ) {
        test(name = "Instantiating with a infinite connection timeout throws an exception") {
            expectThrows<IllegalArgumentException> {
                Publisher(
                    executor = FakePublishExecutor(),
                    demander = ConnectionDemander(),
                    state = MutableStateFlow(ConnectionState.Disconnected),
                    connectionTimeout = Duration.INFINITE,
                )
            }.hasMessage("Connection timeout must be finite")
        }

        test(name = "Instantiating with a negative connection timeout throws an exception") {
            expectThrows<IllegalArgumentException> {
                Publisher(
                    executor = FakePublishExecutor(),
                    demander = ConnectionDemander(),
                    state = MutableStateFlow(ConnectionState.Disconnected),
                    connectionTimeout = (-1).seconds,
                )
            }.hasMessage("Connection timeout must be greater than or equal to 0, but was -1s")
        }

        test(name = "Instantiating with a infinite publish timeout throws an exception") {
            expectThrows<IllegalArgumentException> {
                Publisher(
                    executor = FakePublishExecutor(),
                    demander = ConnectionDemander(),
                    state = MutableStateFlow(ConnectionState.Disconnected),
                    publishTimeout = Duration.INFINITE,
                )
            }.hasMessage("Publish timeout must be finite")
        }

        test(name = "Instantiating with a negative publish timeout throws an exception") {
            expectThrows<IllegalArgumentException> {
                Publisher(
                    executor = FakePublishExecutor(),
                    demander = ConnectionDemander(),
                    state = MutableStateFlow(ConnectionState.Disconnected),
                    publishTimeout = (-1).seconds,
                )
            }.hasMessage("Publish timeout must be greater than or equal to 0, but was -1s")
        }
    }

    testSuite(
        name = "Connection",
    ) {
        test(name = "Publishes immediately when connected") {
            runTest {
                val demander = ConnectionDemander()
                val executor = FakePublishExecutor()

                val state = MutableStateFlow<ConnectionState>(ConnectionState.Connected)

                val publisher = Publisher(
                    executor = executor,
                    demander = demander,
                    state = state,
                )

                publisher.publish(
                    topic = TopicName("home/light/set"),
                    payload = byteArrayOf(1),
                )

                expectThat(executor.messages)
                    .single()
                    .isEqualTo(
                        Message(
                            topic = "home/light/set",
                            payload = byteArrayOf(1),
                        ),
                    )

                expectThat(currentTime)
                    .isEqualTo(0L)

                expectThat(demander.required.first())
                    .isFalse()
            }
        }

        test(name = "Waits for connection before publishing") {
            runTest {
                val demander = ConnectionDemander()
                val executor = FakePublishExecutor()

                val state = MutableStateFlow<ConnectionState>(
                    ConnectionState.Disconnected,
                )

                val publisher = Publisher(
                    executor = executor,
                    demander = demander,
                    state = state,
                )

                val operation = launch {
                    publisher.publish(
                        topic = TopicName("home/light/set"),
                        payload = byteArrayOf(1),
                    )
                }

                runCurrent()

                expectThat(demander.required.first())
                    .isTrue()

                expectThat(executor.messages)
                    .isEmpty()

                expectThat(operation.isActive)
                    .isTrue()

                state.value = ConnectionState.Connected

                operation.join()

                expectThat(executor.messages)
                    .hasSize(1)

                expectThat(demander.required.first())
                    .isFalse()
            }
        }
    }

    testSuite(
        name = "Connection timeout",
    ) {
        test(name = "Fails when connection timeout expires") {
            runTest {
                supervisorScope {
                    val demander = ConnectionDemander()
                    val executor = FakePublishExecutor()

                    val state = MutableStateFlow<ConnectionState>(
                        ConnectionState.Disconnected,
                    )

                    val publisher = Publisher(
                        executor = executor,
                        demander = demander,
                        state = state,
                        connectionTimeout = 5.seconds,
                    )

                    val operation = async {
                        publisher.publish(
                            topic = TopicName("home/light/set"),
                            payload = byteArrayOf(1),
                        )
                    }

                    runCurrent()

                    advanceTimeBy(4999.milliseconds)
                    runCurrent()

                    expectThat(operation.isActive)
                        .isTrue()

                    advanceTimeBy(1.milliseconds)
                    runCurrent()

                    expectThrows<PublishException> { operation.await() }
                        .hasMessage(
                            expected = "Timed out waiting for MQTT connection to be " +
                                "established before publishing to topic 'home/light/set'.",
                        )

                    expectThat(executor.messages)
                        .isEmpty()

                    expectThat(demander.required.first())
                        .isFalse()

                    state.value = ConnectionState.Connected

                    runCurrent()

                    expectThat(executor.messages)
                        .isEmpty()
                }
            }
        }

        test(name = "Releases demand when cancelled while waiting for connection") {
            runTest {
                val demander = ConnectionDemander()
                val executor = FakePublishExecutor()

                val state = MutableStateFlow<ConnectionState>(
                    ConnectionState.Disconnected,
                )

                val publisher = Publisher(
                    executor = executor,
                    demander = demander,
                    state = state,
                )

                val operation = launch {
                    publisher.publish(
                        topic = TopicName("home/light/set"),
                        payload = byteArrayOf(1),
                    )
                }

                runCurrent()

                expectThat(demander.required.first())
                    .isTrue()

                operation.cancelAndJoin()

                expectThat(operation.isCancelled)
                    .isTrue()

                expectThat(demander.required.first())
                    .isFalse()

                expectThat(executor.attempts)
                    .isEqualTo(0)

                state.value = ConnectionState.Connected

                runCurrent()

                expectThat(executor.attempts)
                    .isEqualTo(0)
            }
        }
    }

    testSuite(
        name = "Publishing",
    ) {
        test(name = "Maintains demand until publish completes") {
            runTest {
                val release = CompletableDeferred<Unit>()
                val demander = ConnectionDemander()

                val executor = FakePublishExecutor(
                    onPublish = { _, _ ->
                        release.await()
                    },
                )

                val state = MutableStateFlow<ConnectionState>(
                    ConnectionState.Connected,
                )

                val publisher = Publisher(
                    executor = executor,
                    demander = demander,
                    state = state,
                )

                val operation = launch {
                    publisher.publish(
                        topic = TopicName("home/light/set"),
                        payload = byteArrayOf(1),
                    )
                }

                runCurrent()

                expectThat(demander.required.first())
                    .isTrue()

                expectThat(operation.isActive)
                    .isTrue()

                release.complete(Unit)

                operation.join()

                expectThat(demander.required.first())
                    .isFalse()

                expectThat(executor.messages)
                    .hasSize(1)
            }
        }

        test(name = "Fails when publish timeout expires") {
            runTest {
                supervisorScope {
                    val demander = ConnectionDemander()

                    val executor = FakePublishExecutor(
                        onPublish = { _, _ ->
                            awaitCancellation()
                        },
                    )

                    val state = MutableStateFlow<ConnectionState>(
                        ConnectionState.Connected,
                    )

                    val publisher = Publisher(
                        executor = executor,
                        demander = demander,
                        state = state,
                        publishTimeout = 5.seconds,
                    )

                    val operation = async {
                        publisher.publish(
                            topic = TopicName("home/light/set"),
                            payload = byteArrayOf(1),
                        )
                    }

                    runCurrent()

                    advanceTimeBy(5.seconds)
                    runCurrent()

                    expectThrows<PublishException> { operation.await() }
                        .hasMessage("Timed out waiting for MQTT publish to topic 'home/light/set' to complete.")

                    expectThat(demander.required.first())
                        .isFalse()
                }
            }
        }

        test(name = "Does not retry failed publish") {
            runTest {
                val demander = ConnectionDemander()

                var attempts = 0

                val executor = FakePublishExecutor(
                    onPublish = { _, _ ->
                        attempts++

                        throw PublishException("Publish failed")
                    },
                )

                val state = MutableStateFlow<ConnectionState>(
                    ConnectionState.Connected,
                )

                val publisher = Publisher(
                    executor = executor,
                    demander = demander,
                    state = state,
                )

                expectThrows<PublishException> {
                    publisher.publish(
                        topic = TopicName("home/light/set"),
                        payload = byteArrayOf(1),
                    )
                }.hasMessage("Publish failed")

                expectThat(attempts)
                    .isEqualTo(1)

                expectThat(demander.required.first())
                    .isFalse()
            }
        }

        test(name = "Releases demand when publish is cancelled") {
            runTest {
                val demander = ConnectionDemander()

                val executor = FakePublishExecutor(
                    onPublish = { _, _ ->
                        awaitCancellation()
                    },
                )

                val publisher = Publisher(
                    executor = executor,
                    demander = demander,
                    state = MutableStateFlow(
                        ConnectionState.Connected,
                    ),
                )

                val operation = launch {
                    publisher.publish(
                        topic = TopicName("home/light/set"),
                        payload = byteArrayOf(1),
                    )
                }

                runCurrent()

                expectThat(executor.attempts)
                    .isEqualTo(1)

                expectThat(demander.required.first())
                    .isTrue()

                operation.cancelAndJoin()

                expectThat(operation.isCancelled)
                    .isTrue()

                expectThat(executor.messages)
                    .isEmpty()

                expectThat(demander.required.first())
                    .isFalse()
            }
        }

        test(name = "Propagates unexpected exceptions") {
            runTest {
                val demander = ConnectionDemander()

                val executor = FakePublishExecutor(
                    onPublish = { _, _ ->
                        throw IllegalStateException("Unexpected failure")
                    },
                )

                val publisher = Publisher(
                    executor = executor,
                    demander = demander,
                    state = MutableStateFlow(
                        ConnectionState.Connected,
                    ),
                )

                expectThrows<IllegalStateException> {
                    publisher.publish(
                        topic = TopicName("home/light/set"),
                        payload = byteArrayOf(1),
                    )
                }.hasMessage("Unexpected failure")

                expectThat(executor.attempts)
                    .isEqualTo(1)

                expectThat(demander.required.first())
                    .isFalse()
            }
        }

        test(name = "Propagates cancellation exceptions") {
            runTest {
                val demander = ConnectionDemander()

                val executor = FakePublishExecutor(
                    onPublish = { _, _ ->
                        throw CancellationException("Cancelled")
                    },
                )

                val publisher = Publisher(
                    executor = executor,
                    demander = demander,
                    state = MutableStateFlow(
                        ConnectionState.Connected,
                    ),
                )

                expectThrows<CancellationException> {
                    publisher.publish(
                        topic = TopicName("home/light/set"),
                        payload = byteArrayOf(1),
                    )
                }.hasMessage("Cancelled")

                expectThat(demander.required.first())
                    .isFalse()
            }
        }

        test(name = "Maintains demand for concurrent publish operations") {
            runTest {
                val demander = ConnectionDemander()

                val light = TopicName("home/light/set")
                val thermostat = TopicName("home/thermostat/set")

                val releaseLight = CompletableDeferred<Unit>()
                val releaseThermostat = CompletableDeferred<Unit>()

                val executor = FakePublishExecutor(
                    onPublish = { topic, _ ->
                        when (topic) {
                            light -> releaseLight.await()
                            thermostat -> releaseThermostat.await()
                            else -> error("Unexpected topic")
                        }
                    },
                )

                val publisher = Publisher(
                    executor = executor,
                    demander = demander,
                    state = MutableStateFlow(
                        ConnectionState.Connected,
                    ),
                )

                val first = launch {
                    publisher.publish(
                        topic = light,
                        payload = byteArrayOf(1),
                    )
                }

                val second = launch {
                    publisher.publish(
                        topic = thermostat,
                        payload = byteArrayOf(2),
                    )
                }

                runCurrent()

                expectThat(executor.attempts)
                    .isEqualTo(2)

                expectThat(demander.required.first())
                    .isTrue()

                releaseLight.complete(Unit)

                first.join()

                expectThat(executor.messages)
                    .hasSize(1)

                expectThat(demander.required.first())
                    .isTrue()

                expectThat(second.isActive)
                    .isTrue()

                releaseThermostat.complete(Unit)

                second.join()

                expectThat(executor.messages)
                    .hasSize(2)

                expectThat(demander.required.first())
                    .isFalse()
            }
        }

        test(name = "Failed publish does not affect concurrent publish") {
            runTest {
                supervisorScope {
                    val demander = ConnectionDemander()
                    val release = CompletableDeferred<Unit>()

                    val light = TopicName("home/light/set")
                    val thermostat = TopicName("home/thermostat/set")

                    val executor = FakePublishExecutor(
                        onPublish = { topic, _ ->
                            if (topic == light) {
                                throw PublishException("Publish failed")
                            }

                            release.await()
                        },
                    )

                    val publisher = Publisher(
                        executor = executor,
                        demander = demander,
                        state = MutableStateFlow(
                            ConnectionState.Connected,
                        ),
                    )

                    val first = async {
                        publisher.publish(
                            topic = light,
                            payload = byteArrayOf(1),
                        )
                    }

                    val second = launch {
                        publisher.publish(
                            topic = thermostat,
                            payload = byteArrayOf(2),
                        )
                    }

                    runCurrent()

                    expectThrows<PublishException> {
                        first.await()
                    }.hasMessage("Publish failed")

                    expectThat(second.isActive)
                        .isTrue()

                    expectThat(demander.required.first())
                        .isTrue()

                    release.complete(Unit)

                    second.join()

                    expectThat(executor.messages)
                        .isEqualTo(
                            listOf(
                                Message(
                                    topic = thermostat.value,
                                    payload = byteArrayOf(2),
                                ),
                            ),
                        )

                    expectThat(demander.required.first())
                        .isFalse()
                }
            }
        }
    }

    test(name = "Does not translate caller timeout into publish exception") {
        runTest {
            val demander = ConnectionDemander()

            val executor = FakePublishExecutor(
                onPublish = { _, _ ->
                    awaitCancellation()
                },
            )

            val publisher = Publisher(
                executor = executor,
                demander = demander,
                state = MutableStateFlow(
                    ConnectionState.Connected,
                ),
                publishTimeout = 10.seconds,
            )

            expectThrows<TimeoutCancellationException> {
                withTimeout(2.seconds) {
                    publisher.publish(
                        topic = TopicName("home/light/set"),
                        payload = byteArrayOf(1),
                    )
                }
            }

            expectThat(currentTime)
                .isEqualTo(2000L)

            expectThat(demander.required.first())
                .isFalse()
        }
    }

    test(name = "Copies payload before waiting for connection") {
        runTest {
            val demander = ConnectionDemander()
            val executor = FakePublishExecutor()

            val state = MutableStateFlow<ConnectionState>(
                ConnectionState.Disconnected,
            )

            val publisher = Publisher(
                executor = executor,
                demander = demander,
                state = state,
            )

            val payload = byteArrayOf(1, 2, 3)

            val operation = launch {
                publisher.publish(
                    topic = TopicName("home/light/set"),
                    payload = payload,
                )
            }

            runCurrent()

            expectThat(executor.attempts)
                .isEqualTo(0)

            payload[0] = 42
            payload[1] = 43
            payload[2] = 44

            state.value = ConnectionState.Connected

            operation.join()

            expectThat(executor.messages)
                .isEqualTo(
                    listOf(
                        Message(
                            topic = "home/light/set",
                            payload = byteArrayOf(1, 2, 3),
                        ),
                    ),
                )
        }
    }

    test(name = "Allows empty payload") {
        runTest {
            val executor = FakePublishExecutor()

            val publisher = Publisher(
                executor = executor,
                demander = ConnectionDemander(),
                state = MutableStateFlow(
                    ConnectionState.Connected,
                ),
            )

            publisher.publish(
                topic = TopicName("home/light/set"),
                payload = byteArrayOf(),
            )

            expectThat(executor.messages)
                .isEqualTo(
                    listOf(
                        Message(
                            topic = "home/light/set",
                            payload = byteArrayOf(),
                        ),
                    ),
                )
        }
    }
}

private class FakePublishExecutor(
    private val onPublish: suspend (TopicName, ByteArray) -> Unit = { _, _ -> },
) : PublishExecutor {
    private val published = mutableListOf<Message>()

    val messages: List<Message>
        get() = published.toList()

    var attempts = 0
        private set

    override suspend fun publish(
        topic: TopicName,
        payload: ByteArray,
    ) {
        attempts++

        onPublish.invoke(topic, payload)

        published.add(
            Message(
                topic = topic.value,
                payload = payload.copyOf(),
            ),
        )
    }
}
