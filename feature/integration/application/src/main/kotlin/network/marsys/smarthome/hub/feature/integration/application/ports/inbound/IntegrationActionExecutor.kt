package network.marsys.smarthome.hub.feature.integration.application.ports.inbound

import network.marsys.smarthome.hub.feature.entity.domain.event.EntityActionAccepted

fun interface IntegrationActionExecutor {
    suspend fun execute(action: EntityActionAccepted): Result

    sealed interface Result {
        data object Claimed : Result
        data object NotResponsible : Result
    }
}
