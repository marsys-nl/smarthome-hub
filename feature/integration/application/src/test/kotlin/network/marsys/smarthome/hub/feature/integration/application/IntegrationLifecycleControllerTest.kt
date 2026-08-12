package network.marsys.smarthome.hub.feature.integration.application

import app.cash.turbine.test
import de.infix.testBalloon.framework.core.testSuite
import dev.nmarsman.expect.api.expectThat
import dev.nmarsman.expect.assertions.isA
import dev.nmarsman.expect.assertions.isEqualTo
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import network.marsys.smarthome.hub.feature.integration.domain.Integration

val IntegrationLifecycleControllerTest by testSuite(
    name = "Integration lifecycle controller tests",
) {
    testSuite(name = "Starting integration lifecycle controller") {
        test(name = "Starting an integration lifecycle controller succeeds if the integration has the stopped status") {
            val controller = IntegrationLifecycleController()

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

        listOf(
            Integration.Status.Starting,
            Integration.Status.Running,
            Integration.Status.Degraded,
            Integration.Status.Stopping,
        ).forEach {
            test(name = "Starting an integration lifecycle controller returns if the integration has the $it status") {
                val controller = IntegrationLifecycleController(
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

        test(name = "Starting an integration lifecycle controller that throws an error sets the status to failed") {
            val controller = IntegrationLifecycleController(
                onStart = { error("Some error") },
            )

            controller.status.test {
                expectThat(awaitItem())
                    .isEqualTo(Integration.Status.Stopped)

                try {
                    controller.start()
                } catch (_: Throwable) {}

                expectThat(awaitItem())
                    .isEqualTo(Integration.Status.Starting)

                expectThat(awaitItem())
                    .isA<Integration.Status.Failed>()
            }
        }
    }

    testSuite(name = "Stopping integration lifecycle controller") {
        listOf(
            Integration.Status.Starting,
            Integration.Status.Running,
            Integration.Status.Degraded,
        ).forEach {
            test(name = "Stopping an integration lifecycle controller succeeds if the integration has the $it status") {
                val controller = IntegrationLifecycleController(
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

        test(name = "Stopping an integration lifecycle controller returns if the integration has the stopped status") {
            val controller = IntegrationLifecycleController()

            controller.status.test {
                expectThat(awaitItem())
                    .isEqualTo(Integration.Status.Stopped)

                controller.stop()

                expectNoEvents()
            }
        }

        test(name = "Stopping an integration lifecycle controller returns if the integration has the stopping status") {
            val controller = IntegrationLifecycleController(
                initialStatus = Integration.Status.Stopping,
            )

            controller.status.test {
                expectThat(awaitItem())
                    .isEqualTo(Integration.Status.Stopping)

                controller.stop()

                expectNoEvents()
            }
        }

        test(name = "Stopping an integration lifecycle controller returns if the integration has the failed status") {
            val controller = IntegrationLifecycleController(
                initialStatus = Integration.Status.Failed(RuntimeException("Some error")),
            )

            controller.status.test {
                expectThat(awaitItem())
                    .isA<Integration.Status.Failed>()

                controller.stop()

                expectNoEvents()
            }
        }

        test(name = "Stopping an integration lifecycle controller that throws an error sets the status to failed") {
            val controller = IntegrationLifecycleController(
                initialStatus = Integration.Status.Running,
                onStop = { error("Some error") },
            )

            controller.status.test {
                expectThat(awaitItem())
                    .isEqualTo(Integration.Status.Running)

                try {
                    controller.stop()
                } catch (_: Throwable) {}

                expectThat(awaitItem())
                    .isEqualTo(Integration.Status.Stopping)

                expectThat(awaitItem())
                    .isA<Integration.Status.Failed>()

                expectNoEvents()
            }
        }

        test(name = "Stopping an integration lifecycle controller that throws an error on preparation sets the status to failed") {
            val controller = IntegrationLifecycleController(
                initialStatus = Integration.Status.Running,
                onPrepareStop = { error("Some error") },
            )

            controller.status.test {
                expectThat(awaitItem())
                    .isEqualTo(Integration.Status.Running)

                try {
                    controller.stop()
                } catch (_: Throwable) {}

                expectThat(awaitItem())
                    .isEqualTo(Integration.Status.Stopping)

                expectThat(awaitItem())
                    .isA<Integration.Status.Failed>()

                expectNoEvents()
            }
        }

        test(name = "Stopping an integration lifecycle controller that is just starting succeeds") {
            val controller = IntegrationLifecycleController(
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
