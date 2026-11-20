package network.marsys.smarthome.hub.feature.entity.domain.decider

import network.marsys.smarthome.domain.entity.Entity
import network.marsys.smarthome.domain.entity.Light
import network.marsys.smarthome.hub.feature.entity.domain.EntityActionDecision
import network.marsys.smarthome.hub.feature.entity.domain.EntityActionValidation

internal sealed interface EntityActionValidator<E : Entity> {
    fun validate(entity: E, action: Entity.Action): EntityActionValidation

    companion object {
        fun validate(entity: Entity, action: Entity.Action): EntityActionValidation {
            if (entity.identifier != action.entity) {
                return EntityActionValidation.Rejected(
                    reason = EntityActionDecision.Reason.MismatchedEntity,
                )
            }

            return when (entity) {
                is Light ->
                    LightEntityActionValidator.validate(entity, action)

                else ->
                    EntityActionValidation.Rejected(
                        reason = EntityActionDecision.Reason.Unsupported,
                    )
            }
        }
    }
}
