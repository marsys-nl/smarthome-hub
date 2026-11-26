package network.marsys.smarthome.hub.infrastructure.mqtt.internal

import de.infix.testBalloon.framework.core.testSuite
import dev.nmarsman.expect.api.expectThat
import dev.nmarsman.expect.api.expectThrows
import dev.nmarsman.expect.assertions.containsExactly
import dev.nmarsman.expect.assertions.hasMessage
import dev.nmarsman.expect.assertions.isEmpty
import dev.nmarsman.expect.assertions.isEqualTo
import dev.nmarsman.expect.assertions.isTrue
import dev.nmarsman.expect.assertions.single
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
val SubscriptionManagerTest by testSuite(
    name = "Subscription manager tests",
) {
    test(name = "Does not subscribe without required filters") {
        runTest {
            val registry = SubscriptionRegistry()
            val executor = FakeSubscriptionExecutor()

            val manager = SubscriptionManager(
                executor = executor,
                registry = registry,
            )

            backgroundScope.launch {
                manager.run()
            }

            runCurrent()

            expectThat(executor.subscriptions)
                .isEmpty()

            expectThat(executor.unsubscriptions)
                .isEmpty()
        }
    }

    test(name = "Subscribes to required filters") {
        runTest {
            val registry = SubscriptionRegistry()
            val executor = FakeSubscriptionExecutor()

            val manager = SubscriptionManager(
                executor = executor,
                registry = registry,
            )

            backgroundScope.launch {
                manager.run()
            }

            val filter = TopicFilter("home/light")

            val collector = launch {
                registry.subscribe(setOf(filter))
                    .collect()
            }

            runCurrent()

            expectThat(executor.subscriptions)
                .single()
                .isEqualTo(filter)

            expectThat(executor.unsubscriptions)
                .isEmpty()

            collector.cancelAndJoin()
        }
    }

    test(name = "Does not subscribe twice to the same filter") {
        runTest {
            val registry = SubscriptionRegistry()
            val executor = FakeSubscriptionExecutor()

            val manager = SubscriptionManager(
                executor = executor,
                registry = registry,
            )

            backgroundScope.launch {
                manager.run()
            }

            val filter = TopicFilter("home/light")

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

            expectThat(executor.subscriptions)
                .single()
                .isEqualTo(filter)

            first.cancelAndJoin()
            second.cancelAndJoin()
        }
    }

    test(name = "Unsubscribes when last collector completes") {
        runTest {
            val registry = SubscriptionRegistry()
            val executor = FakeSubscriptionExecutor()

            val manager = SubscriptionManager(
                executor = executor,
                registry = registry,
            )

            backgroundScope.launch {
                manager.run()
            }

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

            first.cancelAndJoin()
            runCurrent()

            expectThat(executor.unsubscriptions)
                .isEmpty()

            second.cancelAndJoin()
            runCurrent()

            expectThat(executor.unsubscriptions)
                .isEqualTo(listOf(filter))
        }
    }

    test(name = "Only synchronizes changed filters") {
        runTest {
            val registry = SubscriptionRegistry()
            val executor = FakeSubscriptionExecutor()

            val manager = SubscriptionManager(
                executor = executor,
                registry = registry,
            )

            backgroundScope.launch {
                manager.run()
            }

            val light = TopicFilter("home/light")
            val temperature = TopicFilter("home/temperature")
            val humidity = TopicFilter("home/humidity")

            val first = launch {
                registry.subscribe(setOf(light, temperature))
                    .collect()
            }

            runCurrent()

            val second = launch {
                registry.subscribe(setOf(temperature, humidity))
                    .collect()
            }

            runCurrent()

            first.cancelAndJoin()
            runCurrent()

            expectThat(executor.subscriptions)
                .isEqualTo(listOf(light, temperature, humidity))

            expectThat(executor.unsubscriptions)
                .isEqualTo(listOf(light))

            second.cancelAndJoin()
            runCurrent()

            expectThat(executor.unsubscriptions.toSet())
                .isEqualTo(setOf(light, temperature, humidity))
        }
    }

    test(name = "Completes pending subscription before synchronizing new filters") {
        runTest {
            val completed = CompletableDeferred<Unit>()

            val light = TopicFilter("home/light")
            val temperature = TopicFilter("home/temperature")

            val registry = SubscriptionRegistry()

            val executor = FakeSubscriptionExecutor(
                onSubscribe = { filter ->
                    if (filter == light) {
                        completed.await()
                    }
                },
            )

            val manager = SubscriptionManager(
                executor = executor,
                registry = registry,
            )

            backgroundScope.launch {
                manager.run()
            }

            val first = launch {
                registry.subscribe(setOf(light))
                    .collect()
            }

            runCurrent()

            val second = launch {
                registry.subscribe(setOf(temperature))
                    .collect()
            }

            runCurrent()

            expectThat(executor.subscriptions)
                .isEmpty()

            completed.complete(Unit)
            runCurrent()

            expectThat(executor.subscriptions)
                .isEqualTo(listOf(light, temperature))

            first.cancelAndJoin()
            second.cancelAndJoin()
        }
    }

    test(name = "Unsubscribes when demand disappears during subscription") {
        runTest {
            val completed = CompletableDeferred<Unit>()

            val registry = SubscriptionRegistry()
            val executor = FakeSubscriptionExecutor(
                onSubscribe = {
                    completed.await()
                },
            )

            val manager = SubscriptionManager(
                executor = executor,
                registry = registry,
            )

            backgroundScope.launch {
                manager.run()
            }

            val filter = TopicFilter("home/light")

            val collector = launch {
                registry.subscribe(setOf(filter))
                    .collect()
            }

            runCurrent()

            collector.cancelAndJoin()
            runCurrent()

            expectThat(executor.subscriptions)
                .isEmpty()

            completed.complete(Unit)
            runCurrent()

            expectThat(executor.subscriptions)
                .isEqualTo(listOf(filter))

            expectThat(executor.unsubscriptions)
                .isEqualTo(listOf(filter))
        }
    }

    test(name = "Restores required subscriptions when restarted") {
        runTest {
            val registry = SubscriptionRegistry()
            val executor = FakeSubscriptionExecutor()

            val manager = SubscriptionManager(
                executor = executor,
                registry = registry,
            )

            val filter = TopicFilter("home/light")

            val collector = launch {
                registry.subscribe(setOf(filter))
                    .collect()
            }

            val first = launch {
                manager.run()
            }

            runCurrent()

            expectThat(executor.subscriptions)
                .isEqualTo(listOf(filter))

            first.cancelAndJoin()

            expectThat(registry.required.value)
                .isEqualTo(setOf(filter))

            expectThat(executor.unsubscriptions)
                .isEmpty()

            val second = launch {
                manager.run()
            }

            runCurrent()

            expectThat(executor.subscriptions)
                .isEqualTo(listOf(filter, filter))

            second.cancelAndJoin()
            collector.cancelAndJoin()
        }
    }

    test(name = "Rejects collectors when subscription fails") {
        runTest {
            supervisorScope {
                val registry = SubscriptionRegistry()
                val filter = TopicFilter("home/light")

                val executor = FakeSubscriptionExecutor(
                    onSubscribe = {
                        throw SubscriptionException("Rejected")
                    },
                )

                val manager = SubscriptionManager(
                    executor = executor,
                    registry = registry,
                )

                val runtime = launch {
                    manager.run()
                }

                val collector = async {
                    registry.subscribe(setOf(filter))
                        .collect()
                }

                runCurrent()

                expectThrows<SubscriptionException> { collector.await() }
                    .hasMessage("Rejected")

                expectThat(registry.required.value)
                    .isEmpty()

                expectThat(executor.subscriptions)
                    .isEmpty()

                expectThat(executor.unsubscriptions)
                    .isEmpty()

                expectThat(runtime.isActive)
                    .isTrue()

                runtime.cancelAndJoin()
            }
        }
    }

    test(name = "Skips filters no longer required after rejection") {
        runTest {
            supervisorScope {
                val registry = SubscriptionRegistry()

                val light = TopicFilter("home/light")
                val temperature = TopicFilter("home/temperature")

                val attempts = mutableListOf<TopicFilter>()

                val executor = FakeSubscriptionExecutor(
                    onSubscribe = { filter ->
                        attempts.add(filter)

                        if (filter == light) {
                            throw SubscriptionException("Rejected")
                        }
                    },
                )

                val manager = SubscriptionManager(
                    executor = executor,
                    registry = registry,
                )

                val runtime = launch {
                    manager.run()
                }

                val collector = async {
                    registry.subscribe(setOf(light, temperature))
                        .collect()
                }

                runCurrent()

                expectThrows<SubscriptionException> {
                    collector.await()
                }

                expectThat(attempts)
                    .single()
                    .isEqualTo(light)

                expectThat(executor.subscriptions)
                    .isEmpty()

                expectThat(executor.unsubscriptions)
                    .isEmpty()

                expectThat(registry.required.value)
                    .isEmpty()

                runtime.cancelAndJoin()
            }
        }
    }

    test(name = "Unsubscribes previously accepted filters after rejection") {
        runTest {
            supervisorScope {
                val registry = SubscriptionRegistry()

                val light = TopicFilter("home/light")
                val temperature = TopicFilter("home/temperature")

                val executor = FakeSubscriptionExecutor(
                    onSubscribe = { filter ->
                        if (filter == temperature) {
                            throw SubscriptionException("Rejected")
                        }
                    },
                )

                val manager = SubscriptionManager(
                    executor = executor,
                    registry = registry,
                )

                val runtime = launch {
                    manager.run()
                }

                val collector = async {
                    registry.subscribe(setOf(light, temperature))
                        .collect()
                }

                runCurrent()

                expectThrows<SubscriptionException> {
                    collector.await()
                }

                runCurrent()

                expectThat(executor.subscriptions)
                    .isEqualTo(listOf(light))

                expectThat(executor.unsubscriptions)
                    .isEqualTo(listOf(light))

                expectThat(registry.required.value)
                    .isEmpty()

                runtime.cancelAndJoin()
            }
        }
    }

    test(name = "Continues synchronizing after rejection") {
        runTest {
            supervisorScope {
                val registry = SubscriptionRegistry()

                val light = TopicFilter("home/light")
                val temperature = TopicFilter("home/temperature")

                val executor = FakeSubscriptionExecutor(
                    onSubscribe = { filter ->
                        if (filter == light) {
                            throw SubscriptionException("Rejected")
                        }
                    },
                )

                val manager = SubscriptionManager(
                    executor = executor,
                    registry = registry,
                )

                val runtime = launch {
                    manager.run()
                }

                val first = async {
                    registry.subscribe(setOf(light))
                        .collect()
                }

                val second = launch {
                    registry.subscribe(setOf(temperature))
                        .collect()
                }

                runCurrent()

                expectThrows<SubscriptionException> {
                    first.await()
                }

                expectThat(executor.subscriptions)
                    .isEqualTo(listOf(temperature))

                expectThat(registry.required.value)
                    .isEqualTo(setOf(temperature))

                expectThat(second.isActive)
                    .isTrue()

                expectThat(runtime.isActive)
                    .isTrue()

                second.cancelAndJoin()
                runtime.cancelAndJoin()
            }
        }
    }
}

private class FakeSubscriptionExecutor(
    private val onSubscribe: suspend (TopicFilter) -> Unit = {},
    private val onUnsubscribe: suspend (TopicFilter) -> Unit = {},
) : SubscriptionExecutor {
    private val subscribed = mutableListOf<TopicFilter>()
    private val unsubscribed = mutableListOf<TopicFilter>()

    val subscriptions: List<TopicFilter>
        get() = subscribed.toList()

    val unsubscriptions: List<TopicFilter>
        get() = unsubscribed.toList()

    override suspend fun subscribe(filter: TopicFilter) {
        onSubscribe.invoke(filter)
        subscribed.add(filter)
    }

    override suspend fun unsubscribe(filter: TopicFilter) {
        onUnsubscribe.invoke(filter)
        unsubscribed.add(filter)
    }
}
