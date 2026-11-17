package network.marsys.smarthome.hub.feature.entity.domain.reducer

import network.marsys.smarthome.hub.feature.entity.domain.action.PendingAction
import network.marsys.smarthome.hub.feature.entity.domain.event.EntityActionAccepted
import network.marsys.smarthome.hub.feature.entity.domain.event.EntityActionEvent

object EntityActionReducer {
    fun reduce(
        history: Collection<EntityActionEvent>,
    ): List<PendingAction> =
        history
            .filterIsInstance<EntityActionAccepted>()
            .map { event ->
                PendingAction(
                    action = event.action,
                    acceptedAt = event.occurredAt,
                )
            }
}
