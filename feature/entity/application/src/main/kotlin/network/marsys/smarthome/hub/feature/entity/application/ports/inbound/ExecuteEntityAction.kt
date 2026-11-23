package network.marsys.smarthome.hub.feature.entity.application.ports.inbound

import network.marsys.smarthome.domain.entity.Entity
import network.marsys.smarthome.hub.feature.entity.domain.EntityActionDecision

fun interface ExecuteEntityAction {
    suspend operator fun invoke(action: Entity.Action): Result

    sealed interface Result {
        data object Accepted : Result
        data object NotFound : Result

        data class Rejected(
            val reason: EntityActionDecision.Reason,
        ) : Result
    }
}
