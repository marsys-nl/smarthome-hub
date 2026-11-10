package network.marsys.smarthome.hub.feature.platform.infrastructure

import de.infix.testBalloon.framework.core.testSuite
import dev.nmarsman.expect.api.expectThat
import dev.nmarsman.expect.api.expectThrows
import dev.nmarsman.expect.assertions.containsExactly
import dev.nmarsman.expect.assertions.hasMessage
import dev.nmarsman.expect.assertions.isEqualTo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import network.marsys.smarthome.domain.capability.OnOff
import network.marsys.smarthome.domain.entity.EntityIdentifier
import network.marsys.smarthome.hub.feature.platform.application.PlatformEvent
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant

val InMemoryPlatformEventBusTest by testSuite(
    name = "In-memory platform event bus tests",
) {
    test(name = "Creating a bus with a non-positive buffer capacity fails") {
        expectThrows<IllegalArgumentException> { InMemoryPlatformEventBus(capacity = 0) }
            .hasMessage("Buffer capacity must be greater than 0")
    }

    testSuite(
        name = "Publishing events",
    ) {
        test(name = "Publishing without subscribers succeeds") {
            val bus = InMemoryPlatformEventBus()
            val event = testEvent(1)

            bus.publish(event)
        }

        test(name = "A single subscriber receives a published event") {
            val bus = InMemoryPlatformEventBus()
            val subscription = bus.subscribe()
            val event = testEvent(1)

            try {
                bus.publish(event)

                expectThat(subscription.events.first())
                    .isEqualTo(event)
            } finally {
                subscription.close()
            }
        }

        test(name = "Multiple subscribers receive the same event") {
            val bus = InMemoryPlatformEventBus()
            val first = bus.subscribe()
            val second = bus.subscribe()
            val event = testEvent(1)

            try {
                bus.publish(event)

                expectThat(first.events.first())
                    .isEqualTo(event)

                expectThat(second.events.first())
                    .isEqualTo(event)
            } finally {
                first.close()
                second.close()
            }
        }

        test(name = "Events are received in publication order") {
            val bus = InMemoryPlatformEventBus()
            val subscription = bus.subscribe()

            val events = listOf(
                testEvent(1),
                testEvent(2),
                testEvent(3),
            )

            try {
                events.forEach { bus.publish(it) }

                expectThat(
                    subject = subscription.events
                        .take(events.size)
                        .toList(),
                ).isEqualTo(events)
            } finally {
                subscription.close()
            }
        }
    }

    testSuite(
        name = "Subscription lifecycle",
    ) {
        test(name = "Events are buffered before collection") {
            val bus = InMemoryPlatformEventBus()
            val subscription = bus.subscribe()
            val event = testEvent(1)

            try {
                bus.publish(event)

                expectThat(subscription.events.first())
                    .isEqualTo(event)
            } finally {
                subscription.close()
            }
        }

        test(name = "New subscribers receive only new events") {
            val bus = InMemoryPlatformEventBus()

            bus.publish(testEvent(1))

            val subscription = bus.subscribe()
            val newEvent = testEvent(2)

            try {
                bus.publish(newEvent)

                expectThat(subscription.events.first())
                    .isEqualTo(newEvent)
            } finally {
                subscription.close()
            }
        }

        test(name = "Closing a subscription stops receiving events") {
            val bus = InMemoryPlatformEventBus()
            val subscription = bus.subscribe()

            subscription.close()

            bus.publish(testEvent(1))

            expectThrows<CancellationException> {
                subscription.events.first()
            }
        }

        test(name = "Closing one subscription does not affect others") {
            val bus = InMemoryPlatformEventBus()
            val first = bus.subscribe()
            val second = bus.subscribe()
            val event = testEvent(1)

            try {
                first.close()

                bus.publish(event)

                expectThat(second.events.first())
                    .isEqualTo(event)
            } finally {
                second.close()
            }
        }
    }

    testSuite(
        name = "Buffer management",
    ) {
        test(name = "A full buffer cancels the subscription") {
            val bus = InMemoryPlatformEventBus(capacity = 1)
            val subscription = bus.subscribe()

            try {
                bus.publish(testEvent(1))
                bus.publish(testEvent(2))

                expectThrows<CancellationException> { subscription.events.first() }
                    .hasMessage("Platform event subscription overflow")
            } finally {
                subscription.close()
            }
        }

        @OptIn(ExperimentalCoroutinesApi::class)
        test(name = "A slow subscriber does not block other subscribers") {
            runTest {
                val bus = InMemoryPlatformEventBus(capacity = 1)

                val slow = bus.subscribe()
                val fast = bus.subscribe()

                val firstEvent = testEvent(1)
                val secondEvent = testEvent(2)

                try {
                    // The fast subscriber actively consumes events.
                    val fastEvents = backgroundScope.async {
                        fast.events.take(2).toList()
                    }

                    runCurrent()

                    // Both subscribers receive the first event.
                    bus.publish(firstEvent)
                    runCurrent()

                    // The fast subscriber has consumed the first event.
                    // The slow subscriber has not.
                    bus.publish(secondEvent)
                    runCurrent()

                    // Only the slow subscriber should overflow.
                    expectThrows<CancellationException> {
                        slow.events.first()
                    }

                    expectThat(fastEvents.await())
                        .containsExactly(firstEvent, secondEvent)
                } finally {
                    slow.close()
                    fast.close()
                }
            }
        }
    }

    @Suppress("RunBlockingInSuspendFunction")
    testSuite(
        name = "Concurrent publishing and subscribing",
    ) {

        test(name = "Concurrent publishers deliver all events in the same order") {
            runBlocking {
                val publisherCount = 8
                val eventsPerPublisher = 100
                val events = publisherCount * eventsPerPublisher

                val bus = InMemoryPlatformEventBus(capacity = events)

                val first = bus.subscribe()
                val second = bus.subscribe()

                val ready = Channel<Unit>(publisherCount)
                val start = CompletableDeferred<Unit>()

                try {
                    val publishers = (0 until publisherCount)
                        .map { publisher ->
                            launch(Dispatchers.Default) {
                                ready.send(Unit)
                                start.await()

                                repeat(eventsPerPublisher) { index ->
                                    bus.publish(testEvent(publisher * eventsPerPublisher + index))
                                }
                            }
                        }

                    repeat(publisherCount) {
                        ready.receive()
                    }

                    start.complete(Unit)

                    withTimeout(10_000.milliseconds) {
                        publishers.joinAll()
                    }

                    val firstEvents = withTimeout(10_000.milliseconds) {
                        first.events.take(events).toList()
                    }

                    val secondEvents = withTimeout(10_000.milliseconds) {
                        second.events.take(events).toList()
                    }

                    val expectedEvents = (0 until events)
                        .map(::testEvent)
                        .toSet()

                    expectThat(firstEvents.size)
                        .isEqualTo(events)

                    expectThat(firstEvents.toSet())
                        .isEqualTo(expectedEvents)

                    expectThat(secondEvents)
                        .isEqualTo(firstEvents)
                } finally {
                    first.close()
                    second.close()
                }
            }
        }

        test(name = "Concurrent subscription changes do not affect active subscribers") {
            runBlocking {
                val publisherCount = 4
                val subscriptionWorkerCount = 4
                val eventsPerPublisher = 100
                val events = publisherCount * eventsPerPublisher
                val workers = publisherCount + subscriptionWorkerCount

                val bus = InMemoryPlatformEventBus(capacity = events)
                val subscription = bus.subscribe()

                val ready = Channel<Unit>(workers)
                val start = CompletableDeferred<Unit>()

                try {
                    val publishers = (0 until publisherCount).map { publisher ->
                        launch(Dispatchers.Default) {
                            ready.send(Unit)
                            start.await()

                            repeat(eventsPerPublisher) { index ->
                                bus.publish(testEvent(publisher * eventsPerPublisher + index))
                            }
                        }
                    }

                    val subscriptionWorkers = (0 until subscriptionWorkerCount)
                        .map {
                            launch(Dispatchers.Default) {
                                ready.send(Unit)
                                start.await()

                                repeat(100) {
                                    val subscription = bus.subscribe()
                                    subscription.close()
                                }
                            }
                        }

                    repeat(workers) {
                        ready.receive()
                    }

                    start.complete(Unit)

                    withTimeout(10_000.milliseconds) {
                        (publishers + subscriptionWorkers).joinAll()
                    }

                    val receivedEvents = withTimeout(10_000.milliseconds) {
                        subscription.events
                            .take(events)
                            .toList()
                    }

                    val expectedEvents = (0 until events)
                        .map(::testEvent)
                        .toSet()

                    expectThat(receivedEvents.size)
                        .isEqualTo(events)

                    expectThat(receivedEvents.toSet())
                        .isEqualTo(expectedEvents)
                } finally {
                    subscription.close()
                }
            }
        }

        test(name = "Closing a subscription concurrently is safe") {
            runBlocking {
                val bus = InMemoryPlatformEventBus()
                val subscription = bus.subscribe()

                try {
                    withTimeout(10_000.milliseconds) {
                        coroutineScope {
                            repeat(100) {
                                launch(Dispatchers.Default) {
                                    subscription.close()
                                }
                            }
                        }
                    }

                    subscription.close()

                    bus.publish(testEvent(1))
                } finally {
                    subscription.close()
                }
            }
        }
    }
}

private fun testEvent(number: Int): PlatformEvent =
    PlatformEvent.CapabilityUpdated(
        identifier = EntityIdentifier("light.test-$number"),
        capability = OnOff(current = number % 2 == 0),
        occurredAt = Instant.parse("2026-11-10T09:00:00Z"),
    )
