package network.marsys.smarthome.hub.feature.integration.application

import app.cash.turbine.test
import de.infix.testBalloon.framework.core.testSuite
import dev.nmarsman.expect.api.expectThat
import dev.nmarsman.expect.api.expectThrows
import dev.nmarsman.expect.assertions.containsExactly
import dev.nmarsman.expect.assertions.isA
import dev.nmarsman.expect.assertions.isEqualTo
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import network.marsys.smarthome.hub.feature.integration.domain.Integration
import kotlin.coroutines.cancellation.CancellationException

@OptIn(ExperimentalCoroutinesApi::class)
val IntegrationLifecycleControllerTest by testSuite(
    name = "Integration lifecycle controller tests",
) {
    testSuite(name = "Starting integration lifecycle controller") {
        test(name = "Starting an integration lifecycle controller succeeds if the integration has the stopped status") {
            runTest {
                val controller = IntegrationLifecycleController(
                    scope = backgroundScope,
                )

                controller.status.test {
                    expectThat(awaitItem())
                        .isEqualTo(Integration.Status.Stopped)

                    controller.start()

                    expectThat(awaitItem())
                        .isEqualTo(Integration.Status.Starting)

                    expectThat(awaitItem())
                        .isEqualTo(Integration.Status.Running)
                }
            }
        }

        listOf(
            Integration.Status.Starting,
            Integration.Status.Running,
            Integration.Status.Stopping,
        ).forEach {
            test(name = "Starting an integration lifecycle controller returns if the integration has the $it status") {
                runTest {
                    val controller = IntegrationLifecycleController(
                        scope = backgroundScope,
                        initialStatus = it,
                    )

                    controller.status.test {
                        expectThat(awaitItem())
                            .isEqualTo(it)

                        controller.start()

                        expectNoEvents()
                    }
                }
            }
        }

        listOf(
            Integration.Status.Degraded,
            Integration.Status.Failed(RuntimeException("Some error")),
        ).forEach {
            test(name = "Starting an integration lifecycle controller with the $it status stops it first") {
                runTest {
                    val controller = IntegrationLifecycleController(
                        scope = backgroundScope,
                        initialStatus = it,
                    )

                    controller.status.test {
                        expectThat(awaitItem())
                            .isEqualTo(it)

                        controller.start()

                        expectThat(awaitItem())
                            .isEqualTo(Integration.Status.Stopping)

                        expectThat(awaitItem())
                            .isA<Integration.Status.Stopped>()

                        expectThat(awaitItem())
                            .isA<Integration.Status.Starting>()

                        expectThat(awaitItem())
                            .isA<Integration.Status.Running>()

                        expectNoEvents()
                    }
                }
            }
        }

        test(name = "Starting an integration lifecycle controller that throws an error sets the status to failed") {
            runTest {
                val controller = IntegrationLifecycleController(
                    scope = backgroundScope,
                    onStart = { error("Some error") },
                )

                controller.status.test {
                    expectThat(awaitItem())
                        .isEqualTo(Integration.Status.Stopped)

                    expectThrows<IllegalStateException> {
                        controller.start()
                    }

                    expectThat(awaitItem())
                        .isEqualTo(Integration.Status.Starting)

                    expectThat(awaitItem())
                        .isA<Integration.Status.Failed>()
                }
            }
        }

        test(name = "Starting an integration lifecycle controller waits until initialization completes") {
            runTest {
                val started = CompletableDeferred<Unit>()
                val completed = CompletableDeferred<Unit>()

                val controller = IntegrationLifecycleController(
                    scope = backgroundScope,
                    onStart = {
                        started.complete(Unit)
                        completed.await()
                    },
                )

                val job = launch {
                    controller.start()
                }

                started.await()

                expectThat(controller.status.value)
                    .isEqualTo(Integration.Status.Starting)

                expectThat(job.isCompleted)
                    .isEqualTo(false)

                completed.complete(Unit)

                job.join()

                expectThat(controller.status.value)
                    .isEqualTo(Integration.Status.Running)

                controller.stop()
            }
        }
    }

    testSuite(name = "Stopping integration lifecycle controller") {
        listOf(
            Integration.Status.Starting,
            Integration.Status.Running,
            Integration.Status.Degraded,
            Integration.Status.Failed(RuntimeException("Some error")),
        ).forEach {
            test(name = "Stopping an integration lifecycle controller succeeds if the integration has the $it status") {
                runTest {
                    val controller = IntegrationLifecycleController(
                        scope = backgroundScope,
                        initialStatus = it,
                    )

                    controller.status.test {
                        expectThat(awaitItem())
                            .isEqualTo(it)

                        controller.stop()

                        expectThat(awaitItem())
                            .isEqualTo(Integration.Status.Stopping)

                        expectThat(awaitItem())
                            .isEqualTo(Integration.Status.Stopped)
                    }
                }
            }
        }

        test(name = "Stopping an integration lifecycle controller returns if the integration has the stopped status") {
            runTest {
                val controller = IntegrationLifecycleController(
                    scope = backgroundScope,
                )

                controller.status.test {
                    expectThat(awaitItem())
                        .isEqualTo(Integration.Status.Stopped)

                    controller.stop()

                    expectNoEvents()
                }
            }
        }

        test(name = "Stopping an integration lifecycle controller returns if the integration has the stopping status") {
            runTest {
                val controller = IntegrationLifecycleController(
                    scope = backgroundScope,
                    initialStatus = Integration.Status.Stopping,
                )

                controller.status.test {
                    expectThat(awaitItem())
                        .isEqualTo(Integration.Status.Stopping)

                    controller.stop()

                    expectNoEvents()
                }
            }
        }

        test(name = "Stopping an integration lifecycle controller that throws an error sets the status to failed") {
            runTest {
                val controller = IntegrationLifecycleController(
                    scope = backgroundScope,
                    initialStatus = Integration.Status.Running,
                    onStop = { error("Some error") },
                )

                controller.status.test {
                    expectThat(awaitItem())
                        .isEqualTo(Integration.Status.Running)

                    expectThrows<IllegalStateException> {
                        controller.stop()
                    }

                    expectThat(awaitItem())
                        .isEqualTo(Integration.Status.Stopping)

                    expectThat(awaitItem())
                        .isA<Integration.Status.Failed>()

                    expectNoEvents()
                }
            }
        }

        test(name = "Stopping an integration lifecycle controller that is just starting succeeds") {
            runTest {
                val controller = IntegrationLifecycleController(
                    scope = backgroundScope,
                    onStart = { awaitCancellation() },
                )

                controller.status.test {
                    expectThat(awaitItem())
                        .isEqualTo(Integration.Status.Stopped)

                    val job = launch {
                        controller.start()
                    }

                    expectThat(awaitItem())
                        .isEqualTo(Integration.Status.Starting)

                    controller.stop()

                    expectThat(awaitItem())
                        .isEqualTo(Integration.Status.Stopping)

                    expectThat(awaitItem())
                        .isEqualTo(Integration.Status.Stopped)

                    expectNoEvents()

                    job.join()
                }
            }
        }
    }

    testSuite(name = "Restarting integration lifecycle controller") {
        test(name = "An integration lifecycle controller can be started again after stopping") {
            runTest {
                var starts = 0
                var stops = 0

                val controller = IntegrationLifecycleController(
                    scope = backgroundScope,
                    onStart = { starts++ },
                    onStop = { stops++ },
                )

                repeat(2) {
                    controller.start()

                    expectThat(controller.status.value)
                        .isEqualTo(Integration.Status.Running)

                    controller.stop()

                    expectThat(controller.status.value)
                        .isEqualTo(Integration.Status.Stopped)
                }

                expectThat(starts)
                    .isEqualTo(2)

                expectThat(stops)
                    .isEqualTo(2)
            }
        }

        test(name = "Restarting a running integration stops it before starting it again") {
            runTest {
                val operations = mutableListOf<String>()

                val controller = IntegrationLifecycleController(
                    scope = backgroundScope,
                    onStart = { operations.add("start") },
                    onStop = { operations.add("stop") },
                )

                controller.start()
                controller.restart()

                expectThat(operations)
                    .containsExactly(listOf("start", "stop", "start"))

                expectThat(controller.status.value)
                    .isEqualTo(Integration.Status.Running)

                controller.stop()
            }
        }

        test(name = "Restarting a stopped integration starts it without stopping it first") {
            runTest {
                var starts = 0
                var stops = 0

                val controller = IntegrationLifecycleController(
                    scope = backgroundScope,
                    onStart = { starts++ },
                    onStop = { stops++ },
                )

                controller.restart()

                expectThat(starts)
                    .isEqualTo(1)

                expectThat(stops)
                    .isEqualTo(0)

                expectThat(controller.status.value)
                    .isEqualTo(Integration.Status.Running)

                controller.stop()
            }
        }
    }

    testSuite(name = "Concurrency and coroutine related tests") {
        test(name = "Child coroutines remain active until the integration lifecycle controller stops") {
            runTest {
                val started = CompletableDeferred<Unit>()
                val stopped = CompletableDeferred<Unit>()

                val controller = IntegrationLifecycleController(
                    scope = backgroundScope,
                    onStart = {
                        launch {
                            try {
                                started.complete(Unit)
                                awaitCancellation()
                            } finally {
                                stopped.complete(Unit)
                            }
                        }
                    },
                )

                controller.start()

                started.await()

                expectThat(controller.status.value)
                    .isEqualTo(Integration.Status.Running)

                expectThat(stopped.isCompleted)
                    .isEqualTo(false)

                controller.stop()

                expectThat(stopped.isCompleted)
                    .isEqualTo(true)

                expectThat(controller.status.value)
                    .isEqualTo(Integration.Status.Stopped)
            }
        }

        test(name = "Concurrent start calls only initialize the integration once") {
            runTest {
                val started = CompletableDeferred<Unit>()
                val completed = CompletableDeferred<Unit>()

                var starts = 0

                val controller = IntegrationLifecycleController(
                    scope = backgroundScope,
                    onStart = {
                        starts++
                        started.complete(Unit)
                        completed.await()
                    },
                )

                val first = launch {
                    controller.start()
                }

                started.await()

                val second = launch {
                    controller.start()
                }

                second.join()

                expectThat(starts)
                    .isEqualTo(1)

                expectThat(controller.status.value)
                    .isEqualTo(Integration.Status.Starting)

                completed.complete(Unit)

                first.join()

                expectThat(starts)
                    .isEqualTo(1)

                expectThat(controller.status.value)
                    .isEqualTo(Integration.Status.Running)

                controller.stop()
            }
        }

        test(name = "Concurrent stop calls only stop the integration once") {
            runTest {
                val started = CompletableDeferred<Unit>()
                val completed = CompletableDeferred<Unit>()

                var stops = 0

                val controller = IntegrationLifecycleController(
                    scope = backgroundScope,
                    onStop = {
                        started.complete(Unit)
                        completed.await()
                        stops++
                    },
                )

                controller.start()

                val first = launch {
                    controller.stop()
                }

                started.await()

                val second = launch {
                    controller.stop()
                }

                runCurrent()

                expectThat(second.isCompleted)
                    .isEqualTo(false)

                expectThat(stops)
                    .isEqualTo(0)

                expectThat(controller.status.value)
                    .isEqualTo(Integration.Status.Stopping)

                completed.complete(Unit)

                first.join()
                second.join()

                expectThat(stops)
                    .isEqualTo(1)

                expectThat(controller.status.value)
                    .isEqualTo(Integration.Status.Stopped)
            }
        }

        test(name = "Starting an integration while stopping waits until the previous lifecycle has stopped") {
            runTest {
                val started = CompletableDeferred<Unit>()
                val completed = CompletableDeferred<Unit>()

                var starts = 0
                var stops = 0

                val controller = IntegrationLifecycleController(
                    scope = backgroundScope,
                    onStart = {
                        starts++
                    },
                    onStop = {
                        started.complete(Unit)
                        completed.await()
                        stops++
                    },
                )

                controller.start()

                val stopJob = launch {
                    controller.stop()
                }

                started.await()

                val startJob = launch {
                    controller.start()
                }

                runCurrent()

                expectThat(startJob.isCompleted)
                    .isEqualTo(false)

                expectThat(starts)
                    .isEqualTo(1)

                expectThat(stops)
                    .isEqualTo(0)

                expectThat(controller.status.value)
                    .isEqualTo(Integration.Status.Stopping)

                completed.complete(Unit)

                stopJob.join()
                startJob.join()

                expectThat(starts)
                    .isEqualTo(2)

                expectThat(stops)
                    .isEqualTo(1)

                expectThat(controller.status.value)
                    .isEqualTo(Integration.Status.Running)

                controller.stop()
            }
        }

        test(name = "Completing initialization while stopping does not change the status to running") {
            runTest {
                val started = CompletableDeferred<Unit>()

                val controller = IntegrationLifecycleController(
                    scope = backgroundScope,
                    onStart = {
                        started.complete(Unit)

                        try {
                            awaitCancellation()
                        } catch (_: CancellationException) {
                            // Intentionally ignore exception
                        }
                    },
                )

                controller.status.test {
                    expectThat(awaitItem())
                        .isEqualTo(Integration.Status.Stopped)

                    val startJob = launch {
                        controller.start()
                    }

                    started.await()

                    expectThat(awaitItem())
                        .isEqualTo(Integration.Status.Starting)

                    controller.stop()

                    expectThat(awaitItem())
                        .isEqualTo(Integration.Status.Stopping)

                    expectThat(awaitItem())
                        .isEqualTo(Integration.Status.Stopped)

                    expectNoEvents()

                    startJob.join()

                    expectThat(startJob.isCancelled)
                        .isEqualTo(true)
                }
            }
        }
    }
}
