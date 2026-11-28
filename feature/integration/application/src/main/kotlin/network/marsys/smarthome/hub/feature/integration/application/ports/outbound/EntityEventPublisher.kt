package network.marsys.smarthome.hub.feature.integration.application.ports.outbound

import network.marsys.smarthome.hub.feature.entity.domain.event.Event

fun interface EntityEventPublisher {
    suspend fun publish(event: Event): Result

    sealed interface Result {
        data object Accepted : Result
        data object Ignored : Result
        data object Rejected : Result
    }
}
