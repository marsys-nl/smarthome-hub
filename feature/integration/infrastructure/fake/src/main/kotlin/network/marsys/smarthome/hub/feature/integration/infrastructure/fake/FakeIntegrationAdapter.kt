package network.marsys.smarthome.hub.feature.integration.infrastructure.fake

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import network.marsys.smarthome.domain.identifiers.IntegrationIdentifier
import network.marsys.smarthome.hub.feature.integration.application.IntegrationAdapter
import network.marsys.smarthome.hub.feature.integration.application.IntegrationLifecycleController
import network.marsys.smarthome.hub.feature.integration.domain.Integration
import kotlin.time.Duration.Companion.seconds

class FakeIntegrationAdapter(
    override val identifier: IntegrationIdentifier = IntegrationIdentifier("integration.fake"),
    initialStatus: Integration.Status = Integration.Status.Stopped,
) : IntegrationAdapter {
    private val lifecycle = IntegrationLifecycleController(
        initialStatus = initialStatus,
        onStart = {
            delay(2.seconds)
        },
        onStop = {
            delay(2.seconds)
        },
    )

    override val status: StateFlow<Integration.Status> = lifecycle.status

    override suspend fun start() = lifecycle.start()
    override suspend fun stop() = lifecycle.stop()
}
