package network.marsys.smarthome.hub.feature.integration.application

import de.infix.testBalloon.framework.core.testSuite
import dev.nmarsman.expect.api.expectDoesNotThrow
import dev.nmarsman.expect.api.expectThat
import dev.nmarsman.expect.api.expectThrows
import dev.nmarsman.expect.assertions.all
import dev.nmarsman.expect.assertions.hasMessage
import dev.nmarsman.expect.assertions.isEqualTo
import dev.nmarsman.expect.assertions.map
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import network.marsys.smarthome.domain.identifiers.IntegrationIdentifier
import network.marsys.smarthome.hub.feature.integration.application.exception.IntegrationNotFoundException
import network.marsys.smarthome.hub.feature.integration.domain.Integration

val IntegrationLifecycleManagerTest by testSuite(
    name = "Integration lifecycle manager tests",
) {
    test(name = "Starting lifecycle manager with no integrations should not throw an exception") {
        val manager = IntegrationLifecycleManager(emptyList())
        expectDoesNotThrow { manager.start() }
    }

    test(name = "Starting integration with an unknown identifier should throw an exception") {
        val integration = FakeIntegration()
        val manager = IntegrationLifecycleManager(listOf(integration))
        expectThrows<IntegrationNotFoundException> { manager.start(IntegrationIdentifier("unknown")) }
            .hasMessage("Integration 'unknown' was not found.")
    }

    test(name = "Stopping integration with an unknown identifier should throw an exception") {
        val integration = FakeIntegration()
        val manager = IntegrationLifecycleManager(listOf(integration))
        expectThrows<IntegrationNotFoundException> { manager.stop(IntegrationIdentifier("unknown")) }
            .hasMessage("Integration 'unknown' was not found.")
    }

    test(name = "Restarting integration with an unknown identifier should throw an exception") {
        val integration = FakeIntegration()
        val manager = IntegrationLifecycleManager(listOf(integration))
        expectThrows<IntegrationNotFoundException> { manager.restart(IntegrationIdentifier("unknown")) }
            .hasMessage("Integration 'unknown' was not found.")
    }

    test(name = "Starting an integration by its identifier succeeds if the integration isn't running") {
        var counter = 0
        val integration = FakeIntegration(
            start = { counter++ },
            initialStatus = Integration.Status.Stopped,
        )

        val manager = IntegrationLifecycleManager(listOf(integration))

        expectThat(integration.status.value)
            .isEqualTo(Integration.Status.Stopped)

        expectDoesNotThrow { manager.start(integration.identifier) }

        expectThat(integration.status.value)
            .isEqualTo(Integration.Status.Running)

        expectThat(counter)
            .isEqualTo(1)
    }

    test(name = "Starting an integration by its identifier fails if the integration is already running") {
        var counter = 0
        val integration = FakeIntegration(
            start = { counter++ },
            initialStatus = Integration.Status.Running,
        )

        val manager = IntegrationLifecycleManager(listOf(integration))

        expectThat(integration.status.value)
            .isEqualTo(Integration.Status.Running)

        expectDoesNotThrow { manager.start(integration.identifier) }

        expectThat(integration.status.value)
            .isEqualTo(Integration.Status.Running)

        expectThat(counter)
            .isEqualTo(0)
    }

    test(name = "Starting an integration that throws an exception should not throw an exception") {
        val integration = FakeIntegration(
            start = {
                throw RuntimeException("Some exception")
            },
        )

        val manager = IntegrationLifecycleManager(listOf(integration))
        expectDoesNotThrow { manager.start() }
    }

    test(name = "Stopping an integration by its identifier succeeds if the integration is running") {
        var counter = 0
        val integration = FakeIntegration(
            stop = { counter++ },
            initialStatus = Integration.Status.Running,
        )

        val manager = IntegrationLifecycleManager(listOf(integration))

        expectThat(integration.status.value)
            .isEqualTo(Integration.Status.Running)

        expectDoesNotThrow { manager.stop(integration.identifier) }

        expectThat(integration.status.value)
            .isEqualTo(Integration.Status.Stopped)

        expectThat(counter)
            .isEqualTo(1)
    }

    test(name = "Stopping an integration by its identifier fails if the integration is already stopped") {
        var counter = 0
        val integration = FakeIntegration(
            stop = { counter++ },
            initialStatus = Integration.Status.Stopped,
        )

        val manager = IntegrationLifecycleManager(listOf(integration))

        expectThat(integration.status.value)
            .isEqualTo(Integration.Status.Stopped)

        expectDoesNotThrow { manager.stop(integration.identifier) }

        expectThat(integration.status.value)
            .isEqualTo(Integration.Status.Stopped)

        expectThat(counter)
            .isEqualTo(0)
    }

    test(name = "Stopping an integration that throws an exception should not throw an exception") {
        val integration = FakeIntegration(
            stop = {
                throw RuntimeException("Some exception")
            },
        )

        val manager = IntegrationLifecycleManager(listOf(integration))
        expectDoesNotThrow { manager.stop() }
    }

    test(name = "Restarting an integration by its identifier succeeds if the integration is running") {
        var counter = 0
        val integration = FakeIntegration(
            stop = { counter++ },
            initialStatus = Integration.Status.Running,
        )

        val manager = IntegrationLifecycleManager(listOf(integration))

        expectThat(integration.status.value)
            .isEqualTo(Integration.Status.Running)

        expectDoesNotThrow { manager.restart(integration.identifier) }

        expectThat(integration.status.value)
            .isEqualTo(Integration.Status.Running)

        expectThat(counter)
            .isEqualTo(1)
    }

    test(name = "Restarting an integration by its identifier succeeds if the integration is already stopped") {
        var counter = 0
        val integration = FakeIntegration(
            stop = { counter++ },
            initialStatus = Integration.Status.Stopped,
        )

        val manager = IntegrationLifecycleManager(listOf(integration))

        expectThat(integration.status.value)
            .isEqualTo(Integration.Status.Stopped)

        expectDoesNotThrow { manager.restart(integration.identifier) }

        expectThat(integration.status.value)
            .isEqualTo(Integration.Status.Running)

        expectThat(counter)
            .isEqualTo(0)
    }

    test(name = "Starting integrations should succeed") {
        val integration = FakeIntegration(
            initialStatus = Integration.Status.Stopped,
        )

        val integrations = listOf(integration, integration, integration)
        val manager = IntegrationLifecycleManager(integrations)

        expectThat(integrations)
            .map { it.status.value }
            .all { isEqualTo(Integration.Status.Stopped) }

        expectDoesNotThrow { manager.start() }

        expectThat(integrations)
            .map { it.status.value }
            .all { isEqualTo(Integration.Status.Running) }
    }

    test(name = "Stopping integrations should succeed") {
        val integration = FakeIntegration(
            initialStatus = Integration.Status.Running,
        )

        val integrations = listOf(integration, integration, integration)
        val manager = IntegrationLifecycleManager(integrations)

        expectThat(integrations)
            .map { it.status.value }
            .all { isEqualTo(Integration.Status.Running) }

        expectDoesNotThrow { manager.stop() }

        expectThat(integrations)
            .map { it.status.value }
            .all { isEqualTo(Integration.Status.Stopped) }
    }
}

private class FakeIntegration(
    private val start: suspend () -> Unit = {},
    private val stop: suspend () -> Unit = {},
    override val identifier: IntegrationIdentifier =
        IntegrationIdentifier("integration.fake"),
    initialStatus: Integration.Status = Integration.Status.Stopped,
) : IntegrationAdapter {
    private val statusStateFlow = MutableStateFlow(initialStatus)
    override val status: StateFlow<Integration.Status> = statusStateFlow

    override suspend fun start() = start.invoke().also {
        statusStateFlow.value = Integration.Status.Running
    }
    override suspend fun stop() = stop.invoke().also {
        statusStateFlow.value = Integration.Status.Stopped
    }
}
