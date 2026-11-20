package network.marsys.smarthome.hub.feature.entity.domain

import network.marsys.smarthome.domain.entity.Entity
import network.marsys.smarthome.hub.feature.entity.domain.action.PendingAction
import network.marsys.smarthome.hub.feature.entity.domain.decider.EntityActionValidator
import network.marsys.smarthome.hub.feature.entity.domain.event.EntityActionAccepted
import network.marsys.smarthome.hub.feature.entity.domain.event.EntityActionEvent
import network.marsys.smarthome.hub.feature.entity.domain.event.Event
import network.marsys.smarthome.hub.feature.entity.domain.reducer.EntityActionReducer
import network.marsys.smarthome.hub.feature.entity.domain.reducer.EntityReducer
import kotlin.time.Clock
import kotlin.time.Instant

class EntityAggregate(
    history: Collection<Event>,
) {
    val entity: Entity =
        EntityReducer.reduce(history = history)

    val pending: List<PendingAction> =
        EntityActionReducer.reduce(
            history = history
                .filterIsInstance<EntityActionEvent>(),
        )

    fun apply(
        action: Entity.Action,
        occurredAt: Instant = Clock.System.now(),
    ): EntityActionDecision {
        if (pending.any { it.action == action }) {
            return EntityActionDecision.Rejected(
                reason = EntityActionDecision.Reason.DuplicateAction,
            )
        }

        return when (val validation = EntityActionValidator.validate(entity, action)) {
            is EntityActionValidation.Accepted ->
                EntityActionDecision.Accepted(
                    events = listOf(
                        EntityActionAccepted(
                            action = action,
                            occurredAt = occurredAt,
                        ),
                    ),
                )

            is EntityActionValidation.Rejected ->
                EntityActionDecision.Rejected(
                    reason = validation.reason,
                )
        }
    }
}
