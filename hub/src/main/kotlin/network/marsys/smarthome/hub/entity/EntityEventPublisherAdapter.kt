package network.marsys.smarthome.hub.entity

import network.marsys.smarthome.hub.feature.entity.application.ports.inbound.ProcessEntityEvent
import network.marsys.smarthome.hub.feature.entity.domain.event.Event
import network.marsys.smarthome.hub.feature.integration.application.ports.outbound.EntityEventPublisher

class EntityEventPublisherAdapter(
    private val processor: ProcessEntityEvent,
) : EntityEventPublisher {
    override suspend fun publish(event: Event): EntityEventPublisher.Result =
        when (processor.invoke(event = event)) {
            is ProcessEntityEvent.Result.Accepted ->
                EntityEventPublisher.Result.Accepted

            is ProcessEntityEvent.Result.Ignored ->
                EntityEventPublisher.Result.Ignored

            is ProcessEntityEvent.Result.Rejected ->
                EntityEventPublisher.Result.Rejected
        }
}
