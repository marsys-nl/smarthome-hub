package network.marsys.smarthome.hub.feature.integration.application.ports.inbound

import network.marsys.smarthome.hub.feature.integration.domain.Integration

interface IntegrationQueries {
    fun all(): Collection<Integration>
}
