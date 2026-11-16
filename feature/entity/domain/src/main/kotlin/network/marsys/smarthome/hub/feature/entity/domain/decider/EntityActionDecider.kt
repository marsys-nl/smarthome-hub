package network.marsys.smarthome.hub.feature.entity.domain.decider

import network.marsys.smarthome.domain.entity.Entity
import network.marsys.smarthome.domain.entity.Light
import network.marsys.smarthome.hub.feature.entity.domain.EntityActionDecision
import network.marsys.smarthome.hub.feature.entity.domain.EntityActionValidation
import network.marsys.smarthome.hub.feature.entity.domain.event.EntityActionAccepted
import kotlin.time.Clock

internal sealed interface EntityActionDecider<E : Entity> {
    fun apply(entity: E, action: Entity.Action): EntityActionValidation

    companion object {
        fun apply(entity: Entity, action: Entity.Action): EntityActionDecision {
            if (entity.identifier != action.entity) {
                return EntityActionDecision.Rejected(
                    reason = EntityActionDecision.Reason.MismatchedEntity,
                )
            }

            return when (val validation = validate(entity, action)) {
                is EntityActionValidation.Accepted ->
                    EntityActionDecision.Accepted(
                        events = listOf(
                            EntityActionAccepted(
                                action = action,
                                occurredAt = Clock.System.now(),
                            ),
                        ),
                    )

                is EntityActionValidation.Rejected ->
                    EntityActionDecision.Rejected(
                        reason = validation.reason,
                    )
            }
        }

        private fun validate(entity: Entity, action: Entity.Action): EntityActionValidation =
            when (entity) {
                is Light ->
                    LightEntityActionDecider.apply(entity, action)

                else ->
                    EntityActionValidation.Rejected(
                        reason = EntityActionDecision.Reason.Unsupported,
                    )
            }
    }
}
