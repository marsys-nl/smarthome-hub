package network.marsys.smarthome.hub.feature.integration.application.ports.inbound

import network.marsys.smarthome.domain.identifiers.IntegrationIdentifier

interface ManageIntegrationLifecycle {
    fun start(identifier: IntegrationIdentifier)
    suspend fun stop(identifier: IntegrationIdentifier)
    suspend fun restart(identifier: IntegrationIdentifier)
}
