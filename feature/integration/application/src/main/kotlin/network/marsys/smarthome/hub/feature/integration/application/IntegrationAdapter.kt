package network.marsys.smarthome.hub.feature.integration.application

import network.marsys.smarthome.hub.feature.integration.application.ports.outbound.IntegrationEventPublisher
import network.marsys.smarthome.hub.feature.integration.application.ports.outbound.IntegrationRuntime
import network.marsys.smarthome.hub.feature.integration.domain.Integration

interface IntegrationAdapter :
    Integration,
    IntegrationEventPublisher,
    IntegrationRuntime
