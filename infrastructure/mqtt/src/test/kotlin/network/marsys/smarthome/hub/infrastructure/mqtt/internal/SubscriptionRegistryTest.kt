package network.marsys.smarthome.hub.infrastructure.mqtt.internal

import de.infix.testBalloon.framework.core.testSuite
import dev.nmarsman.expect.api.expectThat
import dev.nmarsman.expect.api.expectThrows
import dev.nmarsman.expect.assertions.contains
import dev.nmarsman.expect.assertions.containsExactly
import dev.nmarsman.expect.assertions.hasMessage
import dev.nmarsman.expect.assertions.hasSize
import dev.nmarsman.expect.assertions.isA
import dev.nmarsman.expect.assertions.isEmpty
import dev.nmarsman.expect.assertions.isEqualTo
import dev.nmarsman.expect.assertions.isTrue
import dev.nmarsman.expect.assertions.single
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import network.marsys.smarthome.hub.infrastructure.mqtt.Message

@OptIn(ExperimentalCoroutinesApi::class)
val SubscriptionRegistryTest by testSuite(
    name = "Subscription registry tests",
) {
    testSuite(
        name = "Subcription registration",
    ) {
        test(name = "Initially has no required filters") {
            val registry = SubscriptionRegistry()

            expectThat(registry.required.value)
                .isEmpty()
        }

        test(name = "Rejects empty topic filters") {
            val registry = SubscriptionRegistry()

            expectThrows<IllegalArgumentException> { val result = registry.subscribe(filters = emptySet()) }
                .hasMessage("At least one filter must be provided.")
        }

        test(name = "Does not register filters before collection") {
            val registry = SubscriptionRegistry()

            val result = registry.subscribe(filters = setOf(TopicFilter("home/light")))

            expectThat(registry.required.value)
                .isEmpty()
        }

        test(name = "Registers filters during collection") {
            runTest {
                val registry = SubscriptionRegistry()
                val filter = TopicFilter("home/light")

                val collector = launch {
                    registry.subscribe(filters = setOf(filter))
                        .collect()
                }

                runCurrent()

                expectThat(registry.required.value)
                    .single()
                    .isEqualTo(filter)

                collector.cancelAndJoin()

                expectThat(registry.required.value)
                    .isEmpty()
            }
        }

        test(name = "Registers multiple filters") {
            runTest {
                val registry = SubscriptionRegistry()

                val filters = setOf(
                    TopicFilter("home/light"),
                    TopicFilter("home/temperature"),
                    TopicFilter("home/humidity"),
                )

                val collector = launch {
                    registry.subscribe(filters)
                        .collect()
                }

                runCurrent()

                expectThat(registry.required.value)
                    .containsExactly(filters)

                collector.cancelAndJoin()

                expectThat(registry.required.value)
                    .isEmpty()
            }
        }

        test(name = "Retains filters until last collector completes") {
            runTest {
                val registry = SubscriptionRegistry()
                val filter = TopicFilter("home/light")

                val first = launch {
                    registry.subscribe(setOf(filter))
                        .collect()
                }

                val second = launch {
                    registry.subscribe(setOf(filter))
                        .collect()
                }

                runCurrent()

                expectThat(registry.required.value)
                    .isEqualTo(setOf(filter))

                first.cancelAndJoin()

                expectThat(registry.required.value)
                    .isEqualTo(setOf(filter))

                second.cancelAndJoin()

                expectThat(registry.required.value)
                    .isEmpty()
            }
        }

        test(name = "Only removes filters no longer required") {
            runTest {
                val registry = SubscriptionRegistry()

                val light = TopicFilter("home/light")
                val temperature = TopicFilter("home/temperature")
                val humidity = TopicFilter("home/humidity")

                val first = launch {
                    registry.subscribe(setOf(light, temperature))
                        .collect()
                }

                val second = launch {
                    registry.subscribe(setOf(temperature, humidity))
                        .collect()
                }

                runCurrent()

                expectThat(registry.required.value)
                    .isEqualTo(setOf(light, temperature, humidity))

                first.cancelAndJoin()

                expectThat(registry.required.value)
                    .isEqualTo(setOf(temperature, humidity))

                second.cancelAndJoin()

                expectThat(registry.required.value)
                    .isEmpty()
            }
        }

        test(name = "Unregisters filters after receiving first message") {
            runTest {
                val registry = SubscriptionRegistry()
                val filter = TopicFilter("home/light")

                val result = async {
                    registry.subscribe(setOf(filter))
                        .first()
                }

                runCurrent()

                expectThat(registry.required.value)
                    .isEqualTo(setOf(filter))

                val message = Message(
                    topic = "home/light",
                    payload = byteArrayOf(1),
                )

                registry.dispatch(message)

                expectThat(result.await())
                    .isEqualTo(message)

                expectThat(registry.required.value)
                    .isEmpty()
            }
        }

        test(name = "Unregisters filters when collector fails") {
            runTest {
                supervisorScope {
                    val registry = SubscriptionRegistry()
                    val filter = TopicFilter("home/light")

                    val collector = async {
                        registry.subscribe(setOf(filter))
                            .collect {
                                throw IllegalStateException("Consumer failed")
                            }
                    }

                    runCurrent()

                    registry.dispatch(
                        Message(
                            topic = "home/light",
                            payload = byteArrayOf(1),
                        ),
                    )

                    expectThrows<IllegalStateException> {
                        collector.await()
                    }.hasMessage("Consumer failed")

                    expectThat(registry.required.value)
                        .isEmpty()
                }
            }
        }

        test(name = "Only emits when required filters change") {
            runTest {
                val registry = SubscriptionRegistry()
                val filter = TopicFilter("home/light")
                val emissions = mutableListOf<Set<TopicFilter>>()

                backgroundScope.launch {
                    registry.required.collect {
                        emissions.add(it)
                    }
                }

                runCurrent()

                val first = launch {
                    registry.subscribe(setOf(filter))
                        .collect()
                }

                runCurrent()

                val second = launch {
                    registry.subscribe(setOf(filter))
                        .collect()
                }

                runCurrent()

                first.cancelAndJoin()
                runCurrent()

                second.cancelAndJoin()
                runCurrent()

                expectThat(emissions)
                    .hasSize(3)
                    .contains(
                        listOf(
                            emptySet(),
                            setOf(filter),
                            emptySet(),
                        ),
                    )
            }
        }

        test(name = "Copies filters before collection") {
            runTest {
                val registry = SubscriptionRegistry()

                val original = TopicFilter("home/light")
                val replacement = TopicFilter("office/light")

                val filters = mutableSetOf(original)

                val subscription = registry.subscribe(filters)

                filters.clear()
                filters.add(replacement)

                val collector = launch {
                    subscription.collect()
                }

                runCurrent()

                expectThat(registry.required.value)
                    .isEqualTo(setOf(original))

                collector.cancelAndJoin()
            }
        }
    }

    testSuite(
        name = "Distribution",
    ) {
        test(name = "Dispatches without registered collectors") {
            val registry = SubscriptionRegistry()

            registry.dispatch(
                Message(
                    topic = "home/light",
                    payload = byteArrayOf(1),
                ),
            )

            expectThat(registry.required.value)
                .isEmpty()
        }

        test(name = "Dispatches messages to matching collectors") {
            runTest {
                val registry = SubscriptionRegistry()

                val matching = mutableListOf<Message>()
                val nonMatching = mutableListOf<Message>()

                backgroundScope.launch {
                    registry.subscribe(setOf(TopicFilter("home/#")))
                        .collect { matching.add(it) }
                }

                backgroundScope.launch {
                    registry.subscribe(setOf(TopicFilter("office/#")))
                        .collect { nonMatching.add(it) }
                }

                runCurrent()

                val message = Message(
                    topic = "home/light",
                    payload = byteArrayOf(1),
                )

                registry.dispatch(message)

                runCurrent()

                expectThat(matching)
                    .containsExactly(listOf(message))

                expectThat(nonMatching)
                    .isEmpty()
            }
        }

        test(name = "Dispatches messages to multiple interested collectors") {
            runTest {
                val registry = SubscriptionRegistry()

                val first = mutableListOf<Message>()
                val second = mutableListOf<Message>()

                val filters = setOf(TopicFilter("home/light"))

                backgroundScope.launch {
                    registry.subscribe(filters)
                        .collect { first.add(it) }
                }

                backgroundScope.launch {
                    registry.subscribe(filters)
                        .collect { second.add(it) }
                }

                runCurrent()

                val message = Message(
                    topic = "home/light",
                    payload = byteArrayOf(1),
                )

                registry.dispatch(message)

                runCurrent()

                expectThat(first)
                    .containsExactly(listOf(message))

                expectThat(second)
                    .containsExactly(listOf(message))
            }
        }

        test(name = "Dispatches message only once for overlapping filters") {
            runTest {
                val registry = SubscriptionRegistry()
                val received = mutableListOf<Message>()

                backgroundScope.launch {
                    registry.subscribe(
                        setOf(
                            TopicFilter("home/#"),
                            TopicFilter("home/+"),
                            TopicFilter("home/light"),
                        ),
                    ).collect { received.add(it) }
                }

                runCurrent()

                val message = Message(
                    topic = "home/light",
                    payload = byteArrayOf(1),
                )

                registry.dispatch(message)

                runCurrent()

                expectThat(received)
                    .containsExactly(listOf(message))
            }
        }

        test(name = "Preserves sequential message order") {
            runTest {
                val registry = SubscriptionRegistry()
                val received = mutableListOf<Message>()

                backgroundScope.launch {
                    registry.subscribe(setOf(TopicFilter("home/#")))
                        .collect { received.add(it) }
                }

                runCurrent()

                val messages = listOf(
                    Message("home/light", byteArrayOf(1)),
                    Message("home/light", byteArrayOf(2)),
                    Message("home/light", byteArrayOf(3)),
                )

                messages.forEach(registry::dispatch)

                runCurrent()

                expectThat(received)
                    .containsExactly(messages)
            }
        }

        test(name = "Does not dispatch to cancelled collectors") {
            runTest {
                val registry = SubscriptionRegistry()
                val received = mutableListOf<Message>()

                val collector = launch {
                    registry.subscribe(setOf(TopicFilter("home/#")))
                        .collect { received.add(it) }
                }

                runCurrent()

                val first = Message(
                    topic = "home/light",
                    payload = byteArrayOf(1),
                )

                registry.dispatch(first)

                runCurrent()

                collector.cancelAndJoin()

                registry.dispatch(
                    Message(
                        topic = "home/light",
                        payload = byteArrayOf(2),
                    ),
                )

                runCurrent()

                expectThat(received)
                    .containsExactly(listOf(first))
            }
        }

        test(name = "Does not replay previously dispatched messages") {
            runTest {
                val registry = SubscriptionRegistry()
                val received = mutableListOf<Message>()

                registry.dispatch(
                    Message(
                        topic = "home/light",
                        payload = byteArrayOf(1),
                    ),
                )

                backgroundScope.launch {
                    registry.subscribe(setOf(TopicFilter("home/#")))
                        .collect { received.add(it) }
                }

                runCurrent()

                expectThat(received)
                    .isEmpty()

                val message = Message(
                    topic = "home/light",
                    payload = byteArrayOf(2),
                )

                registry.dispatch(message)

                runCurrent()

                expectThat(received)
                    .containsExactly(listOf(message))
            }
        }
    }

    testSuite(
        name = "Channel backpressure",
    ) {
        test(name = "Closes subscription when buffer is full") {
            runTest {
                supervisorScope {
                    val registry = SubscriptionRegistry()
                    val release = CompletableDeferred<Unit>()

                    val collector = async {
                        registry.subscribe(setOf(TopicFilter("home/light")))
                            .buffer(1)
                            .collect {
                                release.await()
                            }
                    }

                    runCurrent()

                    registry.dispatch(
                        Message("home/light", byteArrayOf(1)),
                    )

                    runCurrent()

                    registry.dispatch(
                        Message("home/light", byteArrayOf(2)),
                    )

                    registry.dispatch(
                        Message("home/light", byteArrayOf(3)),
                    )

                    registry.dispatch(
                        Message("home/light", byteArrayOf(4)),
                    )

                    release.complete(Unit)

                    expectThrows<IllegalStateException> {
                        collector.await()
                    }.hasMessage("MQTT subscription buffer is full.")

                    expectThat(registry.required.value)
                        .isEmpty()
                }
            }
        }

        test(name = "Slow collector does not affect other collectors") {
            runTest {
                supervisorScope {
                    val registry = SubscriptionRegistry()
                    val release = CompletableDeferred<Unit>()

                    val received = mutableListOf<Message>()
                    val filter = TopicFilter("home/light")

                    val slow = async {
                        registry.subscribe(setOf(filter))
                            .buffer(1)
                            .collect {
                                release.await()
                            }
                    }

                    val fast = launch {
                        registry.subscribe(setOf(filter))
                            .buffer(1)
                            .collect {
                                received.add(it)
                            }
                    }

                    runCurrent()

                    val messages = listOf(
                        Message("home/light", byteArrayOf(1)),
                        Message("home/light", byteArrayOf(2)),
                        Message("home/light", byteArrayOf(3)),
                        Message("home/light", byteArrayOf(4)),
                    )

                    messages.forEach { message ->
                        registry.dispatch(message)

                        runCurrent()
                    }

                    release.complete(Unit)

                    expectThrows<IllegalStateException> { slow.await() }
                        .hasMessage("MQTT subscription buffer is full.")

                    expectThat(received)
                        .containsExactly(messages)

                    expectThat(registry.required.value)
                        .isEqualTo(setOf(filter))

                    fast.cancelAndJoin()

                    expectThat(registry.required.value)
                        .isEmpty()
                }
            }
        }
    }

    testSuite(
        name = "Rejection",
    ) {
        test(name = "Rejects collector interested in rejected filter") {
            runTest {
                supervisorScope {
                    val registry = SubscriptionRegistry()
                    val filter = TopicFilter("home/light")

                    val cause = SubscriptionException(
                        "Subscription rejected",
                    )

                    val collector = async {
                        registry.subscribe(setOf(filter))
                            .collect()
                    }

                    runCurrent()

                    registry.reject(filter, cause)

                    expectThat(registry.required.value)
                        .isEmpty()

                    expectThrows<SubscriptionException> {
                        collector.await()
                    }.hasMessage("Subscription rejected")
                }
            }
        }

        test(name = "Rejects all collectors interested in rejected filter") {
            runTest {
                supervisorScope {
                    val registry = SubscriptionRegistry()
                    val filter = TopicFilter("home/light")

                    val cause = SubscriptionException(
                        "Subscription rejected",
                    )

                    val first = async {
                        registry.subscribe(setOf(filter))
                            .collect()
                    }

                    val second = async {
                        registry.subscribe(setOf(filter))
                            .collect()
                    }

                    runCurrent()

                    registry.reject(filter, cause)

                    expectThat(registry.required.value)
                        .isEmpty()

                    expectThrows<SubscriptionException> {
                        first.await()
                    }

                    expectThrows<SubscriptionException> {
                        second.await()
                    }
                }
            }
        }

        test(name = "Removes all filters belonging to rejected collector") {
            runTest {
                supervisorScope {
                    val registry = SubscriptionRegistry()

                    val light = TopicFilter("home/light")
                    val temperature = TopicFilter("home/temperature")

                    val collector = async {
                        registry.subscribe(setOf(light, temperature))
                            .collect()
                    }

                    runCurrent()

                    registry.reject(
                        filter = light,
                        cause = SubscriptionException("Rejected"),
                    )

                    expectThat(registry.required.value)
                        .isEmpty()

                    expectThrows<SubscriptionException> {
                        collector.await()
                    }
                }
            }
        }

        test(name = "Does not affect unrelated collectors") {
            runTest {
                supervisorScope {
                    val registry = SubscriptionRegistry()

                    val light = TopicFilter("home/light")
                    val temperature = TopicFilter("home/temperature")

                    val received = mutableListOf<Message>()

                    val rejected = async {
                        registry.subscribe(setOf(light, temperature))
                            .collect()
                    }

                    val remaining = launch {
                        registry.subscribe(setOf(temperature))
                            .collect { received.add(it) }
                    }

                    runCurrent()

                    registry.reject(
                        filter = light,
                        cause = SubscriptionException("Rejected"),
                    )

                    expectThrows<SubscriptionException> {
                        rejected.await()
                    }

                    expectThat(registry.required.value)
                        .isEqualTo(setOf(temperature))

                    val message = Message(
                        topic = "home/temperature",
                        payload = byteArrayOf(1),
                    )

                    registry.dispatch(message)

                    runCurrent()

                    expectThat(received)
                        .containsExactly(listOf(message))

                    expectThat(remaining.isActive)
                        .isTrue()

                    remaining.cancelAndJoin()
                }
            }
        }

        test(name = "Rejects exact filter instead of matching topic filters") {
            runTest {
                val registry = SubscriptionRegistry()

                val wildcard = TopicFilter("home/#")

                val collector = launch {
                    registry.subscribe(setOf(wildcard))
                        .collect()
                }

                runCurrent()

                registry.reject(
                    filter = TopicFilter("home/light"),
                    cause = SubscriptionException("Rejected"),
                )

                runCurrent()

                expectThat(registry.required.value)
                    .isEqualTo(setOf(wildcard))

                expectThat(collector.isActive)
                    .isTrue()

                collector.cancelAndJoin()
            }
        }

        test(name = "Allows new subscription after rejection") {
            runTest {
                supervisorScope {
                    val registry = SubscriptionRegistry()
                    val filter = TopicFilter("home/light")

                    val first = async {
                        registry.subscribe(setOf(filter))
                            .collect()
                    }

                    runCurrent()

                    registry.reject(
                        filter = filter,
                        cause = SubscriptionException("Rejected"),
                    )

                    expectThrows<SubscriptionException> {
                        first.await()
                    }

                    val second = launch {
                        registry.subscribe(setOf(filter))
                            .collect()
                    }

                    runCurrent()

                    expectThat(registry.required.value)
                        .isEqualTo(setOf(filter))

                    expectThat(second.isActive)
                        .isTrue()

                    second.cancelAndJoin()
                }
            }
        }
    }

    testSuite(
        name = "Closing",
    ) {
        test(name = "Closes all active subscriptions") {
            runTest {
                supervisorScope {
                    val registry = SubscriptionRegistry()

                    val first = async {
                        registry.subscribe(
                            setOf(TopicFilter("home/light")),
                        ).collect()
                    }

                    val second = async {
                        registry.subscribe(
                            setOf(TopicFilter("office/#")),
                        ).collect()
                    }

                    runCurrent()

                    registry.close()

                    expectThat(registry.required.value)
                        .isEmpty()

                    expectThrows<ClientClosedException> {
                        first.await()
                    }

                    expectThrows<ClientClosedException> {
                        second.await()
                    }
                }
            }
        }

        test(name = "Rejects collection after close") {
            runTest {
                val registry = SubscriptionRegistry()

                registry.close()

                expectThrows<ClientClosedException> {
                    registry.subscribe(
                        setOf(TopicFilter("home/light")),
                    ).collect()
                }.hasMessage("MQTT client is closed.")

                expectThat(registry.required.value)
                    .isEmpty()
            }
        }

        test(name = "Rejects collection of previously created flow after close") {
            runTest {
                val registry = SubscriptionRegistry()

                val subscription = registry.subscribe(
                    setOf(TopicFilter("home/light")),
                )

                registry.close()

                expectThrows<ClientClosedException> {
                    subscription.collect()
                }

                expectThat(registry.required.value)
                    .isEmpty()
            }
        }

        test(name = "Closing registry multiple times is allowed") {
            val registry = SubscriptionRegistry()

            registry.close()
            registry.close()

            expectThat(registry.required.value)
                .isEmpty()
        }

        test(name = "Preserves cause when registry is closed") {
            runTest {
                supervisorScope {
                    val registry = SubscriptionRegistry()

                    val failure = IllegalStateException("Unexpected runtime failure")
                    val collector = async {
                        registry.subscribe(
                            setOf(TopicFilter("home/light")),
                        ).collect()
                    }

                    runCurrent()

                    registry.close(
                        cause = ClientClosedException(failure),
                    )

                    val exception = try {
                        collector.await()
                        error("Expected collector to fail")
                    } catch (exception: ClientClosedException) {
                        exception
                    }

                    expectThat(exception.cause)
                        .isA<ClientClosedException>()
                        .get(ClientClosedException::cause)
                        .isEqualTo(failure)
                }
            }
        }
    }
}
