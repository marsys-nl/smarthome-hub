package network.marsys.smarthome.hub.feature.entity.domain.reducer

import network.marsys.smarthome.hub.feature.entity.domain.action.PendingAction
import network.marsys.smarthome.hub.feature.entity.domain.event.EntityActionAccepted
import network.marsys.smarthome.hub.feature.entity.domain.event.EntityActionCompleted
import network.marsys.smarthome.hub.feature.entity.domain.event.EntityActionEvent
import network.marsys.smarthome.hub.feature.entity.domain.event.EntityActionFailed

object EntityActionReducer {
    fun reduce(
        history: Collection<EntityActionEvent>,
    ): List<PendingAction> =
        history.fold(emptyList()) { pending, event ->
            when (event) {
                is EntityActionAccepted ->
                    pending + PendingAction(
                        action = event.action,
                        acceptedAt = event.occurredAt,
                    )

                is EntityActionCompleted ->
                    pending.without(accepted = event.accepted)

                is EntityActionFailed ->
                    pending.without(accepted = event.accepted)
            }
        }

    private fun List<PendingAction>.without(
        accepted: EntityActionAccepted,
    ): List<PendingAction> =
        filterNot { pending ->
            pending.action == accepted.action &&
                pending.acceptedAt == accepted.occurredAt
        }
}
