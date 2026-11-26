package network.marsys.smarthome.hub.feature.integration.application

import de.infix.testBalloon.framework.core.testSuite
import dev.nmarsman.expect.api.expectDoesNotThrow
import dev.nmarsman.expect.api.expectThat
import dev.nmarsman.expect.api.expectThrows
import dev.nmarsman.expect.assertions.all
import dev.nmarsman.expect.assertions.hasMessage
import dev.nmarsman.expect.assertions.isEmpty
import dev.nmarsman.expect.assertions.isEqualTo
import dev.nmarsman.expect.assertions.map
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import network.marsys.smarthome.domain.identifiers.IntegrationIdentifier
import network.marsys.smarthome.hub.feature.entity.domain.event.Event
import network.marsys.smarthome.hub.feature.integration.application.exception.IntegrationNotFoundException
import network.marsys.smarthome.hub.feature.integration.domain.Integration

val IntegrationLifecycleManagerTest by testSuite(
    name = "Integration lifecycle manager tests",
) {
    testSuite(name = "Starting integrations") {
        test(name = "Starting lifecycle manager with no integrations should not throw an exception") {
            val manager = IntegrationLifecycleManager(
                integrations = emptyList(),
            )

            expectDoesNotThrow { manager.start() }
        }

        test(name = "Starting integration with an unknown identifier should throw an exception") {
            val identifier = IntegrationIdentifier("integration.unknown")
            val manager = IntegrationLifecycleManager(
                integrations = listOf(FakeIntegration()),
            )

            expectThrows<IntegrationNotFoundException> { manager.start(identifier) }
                .hasMessage("Integration 'integration.unknown' was not found.")
        }

        test(name = "Starting an integration by its identifier delegates to the correct integration") {
            var counter = 0
            val integration = FakeIntegration(
                start = { counter++ },
                initialStatus = Integration.Status.Stopped,
            )

            val other = FakeIntegration(
                identifier = IntegrationIdentifier("integration.other"),
                start = { error("Unexpected start") },
            )

            val manager = IntegrationLifecycleManager(
                integrations = listOf(integration, other),
            )

            manager.start(integration.identifier)

            expectThat(counter)
                .isEqualTo(1)
        }

        test(name = "Starting an integration by its identifier propagates exceptions") {
            val integration = FakeIntegration(
                start = { error("Starting failed") },
            )

            val manager = IntegrationLifecycleManager(
                integrations = listOf(integration),
            )

            expectThrows<IllegalStateException> { manager.start(integration.identifier) }
                .hasMessage("Starting failed")
        }

        test(name = "Starting integrations delegates to all registered integrations") {
            val started = List(3) { CompletableDeferred<Unit>() }

            val integrations = List(3) { index ->
                FakeIntegration(
                    identifier = IntegrationIdentifier("integration.fake-$index"),
                    start = { started[index].complete(Unit) },
                )
            }

            val manager = IntegrationLifecycleManager(
                integrations = integrations,
            )

            manager.start()

            expectThat(started)
                .map(CompletableDeferred<*>::isCompleted)
                .all {
                    isEqualTo(true)
                }
        }

        test(name = "Failing to start an integration does not prevent other integrations from starting") {
            var starts = 0

            val failing = FakeIntegration(
                identifier = IntegrationIdentifier("integration.failing"),
                start = { error("Starting failed") },
            )

            val successful = FakeIntegration(
                identifier = IntegrationIdentifier("integration.successful"),
                start = { starts++ },
            )

            val manager = IntegrationLifecycleManager(
                integrations = listOf(failing, successful),
            )

            expectDoesNotThrow { manager.start() }

            expectThat(starts)
                .isEqualTo(1)
        }
    }

    testSuite(name = "Stopping integrations") {
        test(name = "Stopping lifecycle manager with no integrations should not throw an exception") {
            val manager = IntegrationLifecycleManager(
                integrations = emptyList(),
            )

            expectDoesNotThrow { manager.stop() }
        }

        test(name = "Stopping integration with an unknown identifier should throw an exception") {
            val identifier = IntegrationIdentifier("integration.unknown")
            val manager = IntegrationLifecycleManager(
                integrations = listOf(FakeIntegration()),
            )

            expectThrows<IntegrationNotFoundException> { manager.stop(identifier) }
                .hasMessage("Integration 'integration.unknown' was not found.")
        }

        test(name = "Stopping an integration by its identifier delegates to the correct integration") {
            var stops = 0

            val integration = FakeIntegration(
                stop = { stops++ },
                initialStatus = Integration.Status.Stopped,
            )

            val other = FakeIntegration(
                identifier = IntegrationIdentifier("integration.other"),
                stop = { error("Unexpected stop") },
            )

            val manager = IntegrationLifecycleManager(
                integrations = listOf(other, integration),
            )

            manager.stop(integration.identifier)

            expectThat(stops)
                .isEqualTo(1)
        }

        test(name = "Stopping an integration by its identifier propagates exceptions") {
            val integration = FakeIntegration(
                stop = { error("Stopping failed") },
            )

            val manager = IntegrationLifecycleManager(
                integrations = listOf(integration),
            )

            expectThrows<IllegalStateException> { manager.stop(integration.identifier) }
                .hasMessage("Stopping failed")
        }

        test(name = "Stopping integrations delegates to all registered integrations") {
            val stopped = List(3) { CompletableDeferred<Unit>() }

            val integrations = List(3) { index ->
                FakeIntegration(
                    identifier = IntegrationIdentifier("integration.fake-$index"),
                    stop = { stopped[index].complete(Unit) },
                )
            }

            val manager = IntegrationLifecycleManager(
                integrations = integrations,
            )

            manager.stop()

            expectThat(stopped)
                .map(CompletableDeferred<*>::isCompleted)
                .all {
                    isEqualTo(true)
                }
        }

        test(name = "Failing to stop an integration does not prevent other integrations from stopping") {
            var stops = 0

            val failing = FakeIntegration(
                identifier = IntegrationIdentifier("integration.failing"),
                stop = { error("Stopping failed") },
            )

            val successful = FakeIntegration(
                identifier = IntegrationIdentifier("integration.successful"),
                stop = { stops++ },
            )

            val manager = IntegrationLifecycleManager(
                integrations = listOf(failing, successful),
            )

            expectDoesNotThrow {
                manager.stop()
            }

            expectThat(stops)
                .isEqualTo(1)
        }
    }

    testSuite(name = "Restarting integrations") {
        test(name = "Restarting integration with an unknown identifier should throw an exception") {
            val identifier = IntegrationIdentifier("integration.unknown")
            val manager = IntegrationLifecycleManager(
                integrations = listOf(FakeIntegration()),
            )

            expectThrows<IntegrationNotFoundException> { manager.restart(identifier) }
                .hasMessage("Integration 'integration.unknown' was not found.")
        }

        test(name = "Restarting an integration by its identifier delegates to the correct integration") {
            var restarts = 0

            val integration = FakeIntegration(
                start = { error("Unexpected start") },
                stop = { error("Unexpected stop") },
                restart = { restarts++ },
                initialStatus = Integration.Status.Starting,
            )

            val other = FakeIntegration(
                identifier = IntegrationIdentifier("integration.other"),
                restart = { error("Unexpected restart") },
            )

            val manager = IntegrationLifecycleManager(
                integrations = listOf(other, integration),
            )

            manager.restart(integration.identifier)

            expectThat(restarts)
                .isEqualTo(1)
        }

        test(name = "Restarting an integration by its identifier propagates exceptions") {
            val integration = FakeIntegration(
                restart = { error("Restarting failed") },
            )

            val manager = IntegrationLifecycleManager(
                integrations = listOf(integration),
            )

            expectThrows<IllegalStateException> { manager.restart(integration.identifier) }
                .hasMessage("Restarting failed")
        }
    }

    testSuite(name = "Querying integrations") {
        test(name = "Querying all integrations without any integrations should return an empty list") {
            val manager = IntegrationLifecycleManager(
                integrations = emptyList(),
            )

            expectThat(manager.all())
                .isEmpty()
        }

        for (count in 1..5) {
            test(name = "Querying all integrations with $count integrations should return a list of $count integrations") {
                val integrations = List(count) { index ->
                    FakeIntegration(
                        identifier = IntegrationIdentifier("integration.fake-$index"),
                    )
                }

                val manager = IntegrationLifecycleManager(
                    integrations = integrations,
                )

                expectThat(manager.all())
                    .isEqualTo(integrations)
            }
        }
    }
}

private class FakeIntegration(
    private val start: suspend () -> Unit = {},
    private val stop: suspend () -> Unit = {},
    private val restart: suspend () -> Unit = {},
    override val identifier: IntegrationIdentifier =
        IntegrationIdentifier("integration.fake-integration"),
    initialStatus: Integration.Status = Integration.Status.Stopped,
) : IntegrationAdapter {
    override val events: Flow<Event> = emptyFlow()

    override val status: StateFlow<Integration.Status>
        field = MutableStateFlow(initialStatus)

    override suspend fun start() = start.invoke()
    override suspend fun stop() = stop.invoke()
    override suspend fun restart() = restart.invoke()
}
