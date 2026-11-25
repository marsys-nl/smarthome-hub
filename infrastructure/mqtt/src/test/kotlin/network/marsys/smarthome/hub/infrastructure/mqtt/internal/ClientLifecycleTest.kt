package network.marsys.smarthome.hub.infrastructure.mqtt.internal

import de.infix.testBalloon.framework.core.testSuite
import dev.nmarsman.expect.api.expectThat
import dev.nmarsman.expect.api.expectThrows
import dev.nmarsman.expect.assertions.hasMessage
import dev.nmarsman.expect.assertions.isA
import dev.nmarsman.expect.assertions.isEqualTo
import dev.nmarsman.expect.assertions.isFalse
import dev.nmarsman.expect.assertions.isTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
val ClientLifecycleTest by testSuite(
    name = "Client lifecycle tests",
) {
    test(name = "Executes operation while open") {
        runTest {
            val lifecycle = ClientLifecycle()

            val result = lifecycle.execute { 42 }

            expectThat(result)
                .isEqualTo(42)
        }
    }

    test(name = "Rejects operation after close") {
        runTest {
            val lifecycle = ClientLifecycle()

            var executed = false

            lifecycle.close()

            expectThrows<ClientClosedException> {
                lifecycle.execute {
                    executed = true
                }
            }.hasMessage("MQTT client is closed.")

            expectThat(executed)
                .isFalse()
        }
    }

    test(name = "Allows repeated close") {
        val lifecycle = ClientLifecycle()

        lifecycle.close()
        lifecycle.close()
    }

    test(name = "Cancels active operation when closed") {
        runTest {
            supervisorScope {
                val lifecycle = ClientLifecycle()

                var completed = false

                val operation = async {
                    lifecycle.execute {
                        try {
                            awaitCancellation()
                        } finally {
                            completed = true
                        }
                    }
                }

                runCurrent()

                expectThat(operation.isActive)
                    .isTrue()

                lifecycle.close()

                expectThrows<ClientClosedException> {
                    operation.await()
                }

                expectThat(completed)
                    .isTrue()
            }
        }
    }

    test(name = "Propagates caller cancellation to operation") {
        runTest {
            val lifecycle = ClientLifecycle()

            var completed = false

            val operation = launch {
                lifecycle.execute {
                    try {
                        awaitCancellation()
                    } finally {
                        completed = true
                    }
                }
            }

            runCurrent()

            operation.cancelAndJoin()

            expectThat(completed)
                .isTrue()

            val result = lifecycle.execute {
                42
            }

            expectThat(result)
                .isEqualTo(42)
        }
    }

    test(name = "Propagates operation failure without closing lifecycle") {
        runTest {
            val lifecycle = ClientLifecycle()

            expectThrows<IllegalStateException> {
                lifecycle.execute {
                    throw IllegalStateException("Operation failed")
                }
            }.hasMessage("Operation failed")

            val result = lifecycle.execute { 42 }

            expectThat(result)
                .isEqualTo(42)
        }
    }

    test(name = "Cancels all active operations when closed") {
        runTest {
            supervisorScope {
                val lifecycle = ClientLifecycle()

                var completed = 0

                val operations = List(3) {
                    async {
                        lifecycle.execute {
                            try {
                                awaitCancellation()
                            } finally {
                                completed++
                            }
                        }
                    }
                }

                runCurrent()

                lifecycle.close()

                operations.forEach { operation ->
                    expectThrows<ClientClosedException> {
                        operation.await()
                    }
                }

                expectThat(completed)
                    .isEqualTo(3)
            }
        }
    }

    test(name = "Preserves first close cause") {
        runTest {
            val lifecycle = ClientLifecycle()

            val failure = IllegalStateException(
                "Unexpected runtime failure",
            )

            lifecycle.close(
                ClientClosedException(failure),
            )

            lifecycle.close()

            val exception = try {
                lifecycle.execute {
                    error("Should not be executed")
                }
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
