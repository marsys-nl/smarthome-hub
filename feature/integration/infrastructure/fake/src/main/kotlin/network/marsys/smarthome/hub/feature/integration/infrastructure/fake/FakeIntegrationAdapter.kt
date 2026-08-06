package network.marsys.smarthome.hub.feature.integration.infrastructure.fake

import kotlinx.coroutines.delay
import network.marsys.smarthome.domain.identifiers.IntegrationIdentifier
import network.marsys.smarthome.hub.feature.integration.application.IntegrationAdapter
import kotlin.time.Duration.Companion.seconds

class FakeIntegrationAdapter : IntegrationAdapter {
    override val identifier: IntegrationIdentifier = IntegrationIdentifier("integration.fake")

    override fun start() {
        // No-op for now
    }

    override suspend fun stop() {
        delay(2.seconds)
    }
}
