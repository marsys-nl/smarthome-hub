package network.marsys.smarthome.hub.feature.integration.infrastructure.fake

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import network.marsys.smarthome.domain.identifiers.IntegrationIdentifier
import network.marsys.smarthome.hub.feature.integration.application.IntegrationAdapter
import network.marsys.smarthome.hub.feature.integration.domain.Integration
import kotlin.time.Duration.Companion.seconds

class FakeIntegrationAdapter : IntegrationAdapter {
    override val identifier: IntegrationIdentifier = IntegrationIdentifier("integration.fake")

    private val statusStateFlow = MutableStateFlow<Integration.Status>(Integration.Status.Stopped)
    override val status: StateFlow<Integration.Status> = statusStateFlow.asStateFlow()

    override fun start() {
        statusStateFlow.update { Integration.Status.Running }
    }

    override suspend fun stop() {
        delay(2.seconds)
        statusStateFlow.update { Integration.Status.Stopped }
    }
}
