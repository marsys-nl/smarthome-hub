package network.marsys.smarthome.hub.feature.integration.application.ports.outbound

import kotlinx.coroutines.flow.Flow
import network.marsys.smarthome.hub.feature.entity.domain.event.Event

interface IntegrationEventPublisher {
    val events: Flow<Event>
}
