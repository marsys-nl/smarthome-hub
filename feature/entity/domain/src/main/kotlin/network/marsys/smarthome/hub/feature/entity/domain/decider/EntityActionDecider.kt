package network.marsys.smarthome.hub.feature.entity.domain.decider

import network.marsys.smarthome.domain.entity.Entity
import network.marsys.smarthome.domain.entity.Light
import network.marsys.smarthome.hub.feature.entity.domain.EntityActionDecision

internal sealed interface EntityActionDecider<E : Entity> {
    fun apply(entity: E, action: Entity.Action): EntityActionDecision

    companion object {
        fun apply(entity: Entity, action: Entity.Action): EntityActionDecision {
            if (entity.identifier != action.entity) {
                return EntityActionDecision.Rejected(
                    reason = EntityActionDecision.Reason.MismatchedEntity,
                )
            }

            return when (entity) {
                is Light ->
                    LightEntityActionDecider.apply(entity, action)

                else ->
                    EntityActionDecision.Rejected(
                        reason = EntityActionDecision.Reason.Unsupported,
                    )
            }
        }
    }
}
