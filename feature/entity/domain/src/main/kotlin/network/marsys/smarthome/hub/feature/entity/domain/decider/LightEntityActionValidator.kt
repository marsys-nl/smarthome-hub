package network.marsys.smarthome.hub.feature.entity.domain.decider

import network.marsys.smarthome.domain.capability.Brightness
import network.marsys.smarthome.domain.capability.Capability
import network.marsys.smarthome.domain.capability.OnOff
import network.marsys.smarthome.domain.entity.Entity
import network.marsys.smarthome.domain.entity.Light
import network.marsys.smarthome.hub.feature.entity.domain.EntityActionDecision
import network.marsys.smarthome.hub.feature.entity.domain.EntityActionValidation

internal object LightEntityActionValidator : EntityActionValidator<Light> {
    override fun validate(
        entity: Light,
        action: Entity.Action,
    ): EntityActionValidation = when (val state = entity.state as Light.State) {
        is Light.State.Known -> apply(state, action)
        is Light.State.Unknown -> apply(action)
    }

    private fun apply(state: Light.State.Known, action: Entity.Action): EntityActionValidation =
        when (action) {
            is Brightness.SetBrightness if state.brightness is Capability.Present<*> ->
                EntityActionValidation.Accepted

            is OnOff.Toggle if state.onOff.value.current == (action is OnOff.Toggle.Off) ->
                EntityActionValidation.Accepted

            is OnOff.Toggle ->
                EntityActionValidation.Rejected(
                    reason = EntityActionDecision.Reason.DuplicateState,
                )

            else ->
                EntityActionValidation.Rejected(
                    reason = EntityActionDecision.Reason.Unsupported,
                )
        }

    private fun apply(action: Entity.Action): EntityActionValidation =
        when (action) {
            is Brightness.SetBrightness ->
                EntityActionValidation.Accepted

            is OnOff.Toggle ->
                EntityActionValidation.Accepted

            else ->
                EntityActionValidation.Rejected(
                    reason = EntityActionDecision.Reason.Unsupported,
                )
        }
}
