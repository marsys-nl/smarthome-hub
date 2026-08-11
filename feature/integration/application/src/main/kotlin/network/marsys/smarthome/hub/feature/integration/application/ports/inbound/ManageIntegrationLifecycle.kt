package network.marsys.smarthome.hub.feature.integration.application.ports.inbound

import network.marsys.smarthome.domain.identifiers.IntegrationIdentifier

interface ManageIntegrationLifecycle {
    suspend fun start(identifier: IntegrationIdentifier)
    suspend fun stop(identifier: IntegrationIdentifier)
    suspend fun restart(identifier: IntegrationIdentifier)
}
