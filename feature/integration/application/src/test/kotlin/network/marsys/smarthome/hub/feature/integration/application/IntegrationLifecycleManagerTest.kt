package network.marsys.smarthome.hub.feature.integration.application

import de.infix.testBalloon.framework.core.testSuite
import dev.nmarsman.expect.api.expectDoesNotThrow
import dev.nmarsman.expect.api.expectThat
import dev.nmarsman.expect.api.expectThrows
import dev.nmarsman.expect.assertions.isEqualTo
import dev.nmarsman.expect.assertions.isNotA
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import network.marsys.smarthome.domain.identifiers.IntegrationIdentifier
import kotlin.time.Duration.Companion.seconds

private class FakeIntegration(
    private val start: () -> Unit = {},
    private val stop: suspend () -> Unit = {},
    override val identifier: IntegrationIdentifier =
        IntegrationIdentifier("integration.fake"),
) : IntegrationAdapter {
    override fun start() = start.invoke()
    override suspend fun stop() = stop.invoke()
}

val IntegrationLifecycleManagerTest by testSuite(
    name = "Integration lifecycle manager tests",
) {
    test(name = "Starting lifecycle manager with no integrations should not throw an exception") {
        val manager = IntegrationLifecycleManager(emptyList())
        expectDoesNotThrow { manager.start() }
    }

    test(name = "Starting integration works") {
        val integration = FakeIntegration()
        val manager = IntegrationLifecycleManager(listOf(integration))
        expectDoesNotThrow { manager.start() }
    }

    test(name = "Stopping an integration succeeds") {
        val integration = FakeIntegration()
        val manager = IntegrationLifecycleManager(listOf(integration))
        expectDoesNotThrow { manager.stop() }
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

    test(name = "Stopping an integration that cancels the coroutine throws an exception") {
        val integration = FakeIntegration(
            stop = {
                throw CancellationException("Cancelled the coroutine")
            },
        )

        val manager = IntegrationLifecycleManager(listOf(integration))

        expectThrows<CancellationException> { manager.stop() }
            .isNotA<TimeoutCancellationException>()
    }

    test(name = "Stopping an integration that takes to long throws an exception") {
        val integration = FakeIntegration(
            stop = {
                delay(10.seconds)
            },
        )

        val manager = IntegrationLifecycleManager(listOf(integration))

        expectThrows<TimeoutCancellationException> { manager.stop() }
    }
}
