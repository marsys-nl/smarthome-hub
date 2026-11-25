package network.marsys.smarthome.hub.infrastructure.mqtt.internal

import de.infix.testBalloon.framework.core.testSuite
import dev.nmarsman.expect.api.expectThat
import dev.nmarsman.expect.api.expectThrows
import dev.nmarsman.expect.assertions.hasMessage
import dev.nmarsman.expect.assertions.isEqualTo
import dev.nmarsman.expect.assertions.isFalse
import dev.nmarsman.expect.assertions.isTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import network.marsys.smarthome.hub.infrastructure.mqtt.ConnectionState
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
val ConnectionManagerTest by testSuite(
    name = "Connection manager tests",
) {
    test(name = "Does not connect without demand") {
        runTest {
            val connection = FakeConnection()
            val demander = ConnectionDemander()

            val manager = ConnectionManager(
                connection = connection,
                demander = demander,
                idleTimeout = Duration.ZERO,
            )

            backgroundScope.launch {
                manager.run {
                    // No-op
                }
            }

            runCurrent()

            expectThat(connection.connects)
                .isEqualTo(0)

            expectThat(manager.state.value)
                .isEqualTo(ConnectionState.Disconnected)
        }
    }

    test(name = "Connects when demand becomes active") {
        runTest {
            val connection = FakeConnection()
            val demander = ConnectionDemander()

            val manager = ConnectionManager(
                connection = connection,
                demander = demander,
                idleTimeout = Duration.ZERO,
            )

            backgroundScope.launch {
                manager.run {
                    // No-op
                }
            }

            val consumer = launch {
                demander.demand {
                    awaitCancellation()
                }
            }

            runCurrent()

            expectThat(connection.connects)
                .isEqualTo(1)

            expectThat(manager.state.value)
                .isEqualTo(ConnectionState.Connected)

            consumer.cancelAndJoin()
        }
    }

    test(name = "Disconnects when last consumer completes") {
        runTest {
            val connection = FakeConnection()
            val demander = ConnectionDemander()

            val manager = ConnectionManager(
                connection = connection,
                demander = demander,
                idleTimeout = Duration.ZERO,
            )

            backgroundScope.launch {
                manager.run {
                    // No-op
                }
            }

            val first = launch {
                demander.demand {
                    awaitCancellation()
                }
            }

            val second = launch {
                demander.demand {
                    awaitCancellation()
                }
            }

            runCurrent()

            expectThat(connection.connects)
                .isEqualTo(1)

            first.cancelAndJoin()
            runCurrent()

            expectThat(connection.disconnects)
                .isEqualTo(0)

            expectThat(manager.state.value)
                .isEqualTo(ConnectionState.Connected)

            second.cancelAndJoin()
            runCurrent()

            expectThat(connection.disconnects)
                .isEqualTo(1)

            expectThat(manager.state.value)
                .isEqualTo(ConnectionState.Disconnected)
        }
    }

    test(name = "Disconnects when demand disappears during connection attempt") {
        runTest {
            val completed = CompletableDeferred<Unit>()

            val demander = ConnectionDemander()
            val connection = FakeConnection(
                onConnect = {
                    completed.await()
                },
            )

            val manager = ConnectionManager(
                connection = connection,
                demander = demander,
                idleTimeout = Duration.ZERO,
            )

            backgroundScope.launch {
                manager.run {
                    // No-op
                }
            }

            val consumer = launch {
                demander.demand {
                    awaitCancellation()
                }
            }

            runCurrent()

            expectThat(connection.connects)
                .isEqualTo(1)

            expectThat(connection.connected.value)
                .isFalse()

            consumer.cancelAndJoin()
            runCurrent()

            completed.complete(Unit)
            runCurrent()

            expectThat(connection.connected.value)
                .isFalse()

            expectThat(connection.disconnects)
                .isEqualTo(1)

            expectThat(manager.state.value)
                .isEqualTo(ConnectionState.Disconnected)
        }
    }

    test(name = "Remains connected when disconnect fails") {
        runTest {
            supervisorScope {
                val demander = ConnectionDemander()
                val connection = FakeConnection(
                    onDisconnect = {
                        throw IllegalStateException("Disconnect failed")
                    },
                )

                val manager = ConnectionManager(
                    connection = connection,
                    demander = demander,
                    idleTimeout = Duration.ZERO,
                )

                val job = async {
                    manager.run {
                        // No-op
                    }
                }

                val consumer = launch {
                    demander.demand {
                        awaitCancellation()
                    }
                }

                runCurrent()

                expectThat(manager.state.value)
                    .isEqualTo(ConnectionState.Connected)

                consumer.cancelAndJoin()
                runCurrent()

                expectThrows<IllegalStateException> {
                    job.await()
                }

                expectThat(connection.disconnects)
                    .isEqualTo(1)

                expectThat(manager.state.value)
                    .isEqualTo(ConnectionState.Connected)
            }
        }
    }

    test(name = "Retry delay resets after successful connection") {
        runTest {
            var available = false

            val demander = ConnectionDemander()
            val connection = FakeConnection(
                onConnect = {
                    if (!available) {
                        throw ConnectionException("Broker unavailable")
                    }
                },
            )

            val manager = ConnectionManager(
                connection = connection,
                demander = demander,
                idleTimeout = Duration.ZERO,
            )

            backgroundScope.launch {
                manager.run {
                    // No-op
                }
            }

            val consumer = launch {
                demander.demand {
                    awaitCancellation()
                }
            }

            runCurrent()

            expectThat(connection.connects)
                .isEqualTo(1)

            advanceTimeBy(1.seconds)
            runCurrent()

            expectThat(connection.connects)
                .isEqualTo(2)

            available = true

            advanceTimeBy(2.seconds)
            runCurrent()

            expectThat(connection.connects)
                .isEqualTo(3)

            expectThat(manager.state.value)
                .isEqualTo(ConnectionState.Connected)

            available = false

            connection.simulateConnectionLoss()
            runCurrent()

            expectThat(connection.connects)
                .isEqualTo(4)

            expectThat(manager.state.value)
                .isEqualTo(ConnectionState.Disconnected)

            available = true

            advanceTimeBy(1.seconds)
            runCurrent()

            expectThat(connection.connects)
                .isEqualTo(5)

            expectThat(manager.state.value)
                .isEqualTo(ConnectionState.Connected)

            consumer.cancelAndJoin()
        }
    }

    test(name = "Unexpected exceptions terminate connection management") {
        runTest {
            supervisorScope {
                val demander = ConnectionDemander()
                val connection = FakeConnection(
                    onConnect = {
                        throw IllegalStateException("Unexpected failure")
                    },
                )

                val manager = ConnectionManager(
                    connection = connection,
                    demander = demander,
                    idleTimeout = Duration.ZERO,
                )

                val job = async {
                    manager.run {
                        // No-op
                    }
                }

                val consumer = launch {
                    demander.demand {
                        awaitCancellation()
                    }
                }

                runCurrent()

                expectThrows<IllegalStateException> { job.await() }
                    .hasMessage("Unexpected failure")

                expectThat(connection.connects)
                    .isEqualTo(1)

                expectThat(connection.disconnects)
                    .isEqualTo(0)

                expectThat(manager.state.value)
                    .isEqualTo(ConnectionState.Disconnected)

                consumer.cancelAndJoin()
            }
        }
    }

    test(name = "Disconnects when manager is cancelled") {
        runTest {
            val demander = ConnectionDemander()
            val connection = FakeConnection()

            val manager = ConnectionManager(
                connection = connection,
                demander = demander,
                idleTimeout = Duration.ZERO,
            )

            val job = launch {
                manager.run {
                    // No-op
                }
            }

            val consumer = launch {
                demander.demand {
                    awaitCancellation()
                }
            }

            runCurrent()

            expectThat(manager.state.value)
                .isEqualTo(ConnectionState.Connected)

            job.cancelAndJoin()

            expectThat(connection.disconnects)
                .isEqualTo(1)

            expectThat(connection.connected.value)
                .isFalse()

            expectThat(manager.state.value)
                .isEqualTo(ConnectionState.Disconnected)

            consumer.cancelAndJoin()
        }
    }

    test(name = "Disconnects after idle timeout") {
        runTest {
            val connection = FakeConnection()
            val demander = ConnectionDemander()

            val manager = ConnectionManager(
                connection = connection,
                demander = demander,
                idleTimeout = 5.seconds,
            )

            backgroundScope.launch {
                manager.run {
                    // No-op
                }
            }

            val consumer = launch {
                demander.demand {
                    awaitCancellation()
                }
            }

            runCurrent()

            expectThat(connection.connects)
                .isEqualTo(1)

            consumer.cancelAndJoin()
            runCurrent()

            advanceTimeBy(4999.milliseconds)
            runCurrent()

            expectThat(connection.disconnects)
                .isEqualTo(0)

            expectThat(manager.state.value)
                .isEqualTo(ConnectionState.Connected)

            advanceTimeBy(1.milliseconds)
            runCurrent()

            expectThat(connection.disconnects)
                .isEqualTo(1)

            expectThat(manager.state.value)
                .isEqualTo(ConnectionState.Disconnected)
        }
    }

    test(name = "Retains connection when demand returns during idle timeout") {
        runTest {
            val connection = FakeConnection()
            val demander = ConnectionDemander()

            val manager = ConnectionManager(
                connection = connection,
                demander = demander,
                idleTimeout = 5.seconds,
            )

            backgroundScope.launch {
                manager.run {
                    // No-op
                }
            }

            val first = launch {
                demander.demand {
                    awaitCancellation()
                }
            }

            runCurrent()

            first.cancelAndJoin()
            runCurrent()

            advanceTimeBy(3.seconds)

            val second = launch {
                demander.demand {
                    awaitCancellation()
                }
            }

            runCurrent()

            advanceTimeBy(10.seconds)
            runCurrent()

            expectThat(connection.connects)
                .isEqualTo(1)

            expectThat(connection.disconnects)
                .isEqualTo(0)

            expectThat(manager.state.value)
                .isEqualTo(ConnectionState.Connected)

            second.cancelAndJoin()
            runCurrent()

            advanceTimeBy(5.seconds)
            runCurrent()

            expectThat(connection.disconnects)
                .isEqualTo(1)
        }
    }

    test(name = "Does not reconnect when connection is lost during idle timeout") {
        runTest {
            val connection = FakeConnection()
            val demander = ConnectionDemander()

            val manager = ConnectionManager(
                connection = connection,
                demander = demander,
                idleTimeout = 5.seconds,
            )

            backgroundScope.launch {
                manager.run {
                    // No-op
                }
            }

            val consumer = launch {
                demander.demand {
                    awaitCancellation()
                }
            }

            runCurrent()

            consumer.cancelAndJoin()
            runCurrent()

            advanceTimeBy(2.seconds)

            connection.simulateConnectionLoss()
            runCurrent()

            expectThat(manager.state.value)
                .isEqualTo(ConnectionState.Disconnected)

            expectThat(connection.connects)
                .isEqualTo(1)

            advanceTimeBy(10.seconds)
            runCurrent()

            expectThat(connection.connects)
                .isEqualTo(1)
        }
    }

    test(name = "Ignores idle timeout when manager is cancelled") {
        runTest {
            val connection = FakeConnection()
            val demander = ConnectionDemander()

            val manager = ConnectionManager(
                connection = connection,
                demander = demander,
                idleTimeout = 5.seconds,
            )

            val job = launch {
                manager.run {
                    // No-op
                }
            }

            val consumer = launch {
                demander.demand {
                    awaitCancellation()
                }
            }

            runCurrent()

            consumer.cancelAndJoin()
            runCurrent()

            expectThat(connection.connected.value)
                .isTrue()

            job.cancelAndJoin()

            expectThat(connection.disconnects)
                .isEqualTo(1)

            expectThat(manager.state.value)
                .isEqualTo(ConnectionState.Disconnected)

            expectThat(currentTime)
                .isEqualTo(0L)
        }
    }

    test(name = "Instantiating with a infinite idle timeout throws an exception") {
        expectThrows<IllegalArgumentException> {
            ConnectionManager(
                connection = FakeConnection(),
                demander = ConnectionDemander(),
                idleTimeout = Duration.INFINITE,
            )
        }.hasMessage("Idle timeout must be finite")
    }

    test(name = "Instantiating with a negative idle timeout throws an exception") {
        expectThrows<IllegalArgumentException> {
            ConnectionManager(
                connection = FakeConnection(),
                demander = ConnectionDemander(),
                idleTimeout = (-1).seconds,
            )
        }.hasMessage("Idle timeout must be greater than or equal to 0, but was -1s")
    }

    testSuite(
        name = "Connection sessions",
    ) {
        test(name = "Starts session when connected") {
            runTest {
                val connection = FakeConnection()
                val demander = ConnectionDemander()

                val manager = ConnectionManager(
                    connection = connection,
                    demander = demander,
                    idleTimeout = Duration.ZERO,
                )

                var sessions = 0

                backgroundScope.launch {
                    manager.run {
                        sessions++
                        awaitCancellation()
                    }
                }

                runCurrent()

                expectThat(sessions)
                    .isEqualTo(0)

                val consumer = launch {
                    demander.demand {
                        awaitCancellation()
                    }
                }

                runCurrent()

                expectThat(sessions)
                    .isEqualTo(1)

                expectThat(manager.state.value)
                    .isEqualTo(ConnectionState.Connected)

                consumer.cancelAndJoin()
            }
        }

        test(name = "Does not start session when connection attempt fails") {
            runTest {
                val connection = FakeConnection(
                    onConnect = {
                        throw ConnectionException("Broker unavailable")
                    },
                )

                val demander = ConnectionDemander()

                val manager = ConnectionManager(
                    connection = connection,
                    demander = demander,
                    idleTimeout = Duration.ZERO,
                )

                var sessions = 0

                backgroundScope.launch {
                    manager.run {
                        sessions++
                        awaitCancellation()
                    }
                }

                val consumer = launch {
                    demander.demand {
                        awaitCancellation()
                    }
                }

                runCurrent()

                advanceTimeBy(10.seconds)
                runCurrent()

                expectThat(sessions)
                    .isEqualTo(0)

                consumer.cancelAndJoin()
            }
        }

        test(name = "Starts new session after reconnect") {
            runTest {
                val connection = FakeConnection()
                val demander = ConnectionDemander()

                val manager = ConnectionManager(
                    connection = connection,
                    demander = demander,
                    idleTimeout = Duration.ZERO,
                )

                var started = 0
                var completed = 0

                backgroundScope.launch {
                    manager.run {
                        started++

                        try {
                            awaitCancellation()
                        } finally {
                            completed++
                        }
                    }
                }

                val consumer = launch {
                    demander.demand {
                        awaitCancellation()
                    }
                }

                runCurrent()

                expectThat(started)
                    .isEqualTo(1)

                expectThat(completed)
                    .isEqualTo(0)

                connection.simulateConnectionLoss()

                runCurrent()

                expectThat(started)
                    .isEqualTo(2)

                expectThat(completed)
                    .isEqualTo(1)

                consumer.cancelAndJoin()

                runCurrent()

                expectThat(completed)
                    .isEqualTo(2)
            }
        }

        test(name = "Waits for session cleanup before reconnecting") {
            runTest {
                val connection = FakeConnection()
                val demander = ConnectionDemander()
                val release = CompletableDeferred<Unit>()

                val manager = ConnectionManager(
                    connection = connection,
                    demander = demander,
                    idleTimeout = Duration.ZERO,
                )

                backgroundScope.launch {
                    manager.run {
                        try {
                            awaitCancellation()
                        } finally {
                            withContext(NonCancellable) {
                                release.await()
                            }
                        }
                    }
                }

                val consumer = launch {
                    demander.demand {
                        awaitCancellation()
                    }
                }

                runCurrent()

                expectThat(connection.connects)
                    .isEqualTo(1)

                connection.simulateConnectionLoss()

                runCurrent()

                expectThat(connection.connects)
                    .isEqualTo(1)

                release.complete(Unit)

                runCurrent()

                expectThat(connection.connects)
                    .isEqualTo(2)

                consumer.cancelAndJoin()
            }
        }

        test(name = "Retains session during idle timeout") {
            runTest {
                val connection = FakeConnection()
                val demander = ConnectionDemander()

                val manager = ConnectionManager(
                    connection = connection,
                    demander = demander,
                    idleTimeout = 5.seconds,
                )

                var completed = 0

                backgroundScope.launch {
                    manager.run {
                        try {
                            awaitCancellation()
                        } finally {
                            completed++
                        }
                    }
                }

                val consumer = launch {
                    demander.demand {
                        awaitCancellation()
                    }
                }

                runCurrent()

                consumer.cancelAndJoin()

                runCurrent()

                advanceTimeBy(4999.milliseconds)
                runCurrent()

                expectThat(completed)
                    .isEqualTo(0)

                advanceTimeBy(1.milliseconds)
                runCurrent()

                expectThat(completed)
                    .isEqualTo(1)

                expectThat(connection.disconnects)
                    .isEqualTo(1)
            }
        }

        test(name = "Completes session before disconnecting") {
            runTest {
                var sessionCompleted = false

                val connection = FakeConnection(
                    onDisconnect = {
                        expectThat(sessionCompleted)
                            .isTrue()
                    },
                )

                val demander = ConnectionDemander()

                val manager = ConnectionManager(
                    connection = connection,
                    demander = demander,
                    idleTimeout = Duration.ZERO,
                )

                backgroundScope.launch {
                    manager.run {
                        try {
                            awaitCancellation()
                        } finally {
                            sessionCompleted = true
                        }
                    }
                }

                val consumer = launch {
                    demander.demand {
                        awaitCancellation()
                    }
                }

                runCurrent()

                consumer.cancelAndJoin()

                runCurrent()

                expectThat(connection.disconnects)
                    .isEqualTo(1)
            }
        }

        test(name = "Does not start session when connection is immediately lost") {
            runTest {
                var firstAttempt = true

                val connection = FakeConnection(
                    onConnected = {
                        if (firstAttempt) {
                            firstAttempt = false
                            simulateConnectionLoss()
                        }
                    },
                )

                val demander = ConnectionDemander()

                val manager = ConnectionManager(
                    connection = connection,
                    demander = demander,
                    idleTimeout = Duration.ZERO,
                )

                var sessions = 0

                backgroundScope.launch {
                    manager.run {
                        sessions++

                        awaitCancellation()
                    }
                }

                val consumer = launch {
                    demander.demand {
                        awaitCancellation()
                    }
                }

                runCurrent()

                expectThat(connection.connects)
                    .isEqualTo(2)

                expectThat(sessions)
                    .isEqualTo(1)

                expectThat(manager.state.value)
                    .isEqualTo(ConnectionState.Connected)

                consumer.cancelAndJoin()
            }
        }

        test(name = "Recovers from session failure") {
            runTest {
                val connection = FakeConnection()
                val demander = ConnectionDemander()

                val manager = ConnectionManager(
                    connection = connection,
                    demander = demander,
                    idleTimeout = Duration.ZERO,
                )

                var sessions = 0

                backgroundScope.launch {
                    manager.run {
                        sessions++

                        if (sessions == 1) {
                            throw SessionException("Session failed")
                        }

                        awaitCancellation()
                    }
                }

                val consumer = launch {
                    demander.demand {
                        awaitCancellation()
                    }
                }

                runCurrent()

                expectThat(sessions)
                    .isEqualTo(1)

                expectThat(connection.connects)
                    .isEqualTo(1)

                expectThat(connection.disconnects)
                    .isEqualTo(1)

                expectThat(manager.state.value)
                    .isEqualTo(ConnectionState.Disconnected)

                advanceTimeBy(999.milliseconds)
                runCurrent()

                expectThat(connection.connects)
                    .isEqualTo(1)

                advanceTimeBy(1.milliseconds)
                runCurrent()

                expectThat(connection.connects)
                    .isEqualTo(2)

                expectThat(sessions)
                    .isEqualTo(2)

                expectThat(manager.state.value)
                    .isEqualTo(ConnectionState.Connected)

                consumer.cancelAndJoin()
            }
        }

        test(name = "Increases retry delay after consecutive session failures") {
            runTest {
                val connection = FakeConnection()
                val demander = ConnectionDemander()

                val manager = ConnectionManager(
                    connection = connection,
                    demander = demander,
                    idleTimeout = Duration.ZERO,
                )

                backgroundScope.launch {
                    manager.run {
                        throw SessionException("Session failed")
                    }
                }

                val consumer = launch {
                    demander.demand {
                        awaitCancellation()
                    }
                }

                runCurrent()

                expectThat(connection.connects)
                    .isEqualTo(1)

                advanceTimeBy(1.seconds)
                runCurrent()

                expectThat(connection.connects)
                    .isEqualTo(2)

                advanceTimeBy(1999.milliseconds)
                runCurrent()

                expectThat(connection.connects)
                    .isEqualTo(2)

                advanceTimeBy(1.milliseconds)
                runCurrent()

                expectThat(connection.connects)
                    .isEqualTo(3)

                advanceTimeBy(4.seconds)
                runCurrent()

                expectThat(connection.connects)
                    .isEqualTo(4)

                consumer.cancelAndJoin()
            }
        }

        test(name = "Cancels session recovery when demand disappears") {
            runTest {
                val connection = FakeConnection()
                val demander = ConnectionDemander()

                val manager = ConnectionManager(
                    connection = connection,
                    demander = demander,
                    idleTimeout = Duration.ZERO,
                )

                backgroundScope.launch {
                    manager.run {
                        throw SessionException("Session failed")
                    }
                }

                val consumer = launch {
                    demander.demand {
                        awaitCancellation()
                    }
                }

                runCurrent()

                expectThat(connection.connects)
                    .isEqualTo(1)

                expectThat(connection.disconnects)
                    .isEqualTo(1)

                consumer.cancelAndJoin()
                runCurrent()

                advanceTimeBy(120.seconds)
                runCurrent()

                expectThat(connection.connects)
                    .isEqualTo(1)

                expectThat(manager.state.value)
                    .isEqualTo(ConnectionState.Disconnected)
            }
        }
    }
}

private class FakeConnection(
    private val onConnect: suspend () -> Unit = {},
    private val onDisconnect: suspend () -> Unit = {},
    private val onConnected: suspend FakeConnection.() -> Unit = {},
) : Connection {
    var connects: Int = 0
        private set

    var disconnects: Int = 0
        private set

    override val connected = MutableStateFlow(false)

    override suspend fun connect() {
        connects++
        onConnect.invoke()
        connected.value = true
        onConnected.invoke(this)
    }

    override suspend fun disconnect() {
        disconnects++
        onDisconnect.invoke()
        connected.value = false
    }

    fun simulateConnectionLoss() {
        connected.value = false
    }
}
