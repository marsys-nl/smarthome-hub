package network.marsys.smarthome.hub.feature.entity.domain.decider

import network.marsys.smarthome.domain.capability.Brightness
import network.marsys.smarthome.domain.capability.Capability
import network.marsys.smarthome.domain.capability.OnOff
import network.marsys.smarthome.domain.entity.Entity
import network.marsys.smarthome.domain.entity.Light
import network.marsys.smarthome.hub.feature.entity.domain.EntityActionDecision

object LightEntityActionDecider : EntityActionDecider<Light> {
    override fun apply(
        entity: Light,
        action: Entity.Action,
    ): EntityActionDecision = when (val state = entity.state as Light.State) {
        is Light.State.Known -> apply(state, action)
        is Light.State.Unknown -> apply(action)
    }

    private fun apply(state: Light.State.Known, action: Entity.Action): EntityActionDecision =
        when (action) {
            is Brightness.SetBrightness if state.brightness is Capability.Present<*> ->
                EntityActionDecision.Accepted

            is OnOff.Toggle if state.onOff.value.current == (action is OnOff.Toggle.Off) ->
                EntityActionDecision.Accepted

            is OnOff.Toggle ->
                EntityActionDecision.Rejected(
                    reason = EntityActionDecision.Reason.DuplicateState,
                )

            else ->
                EntityActionDecision.Rejected(
                    reason = EntityActionDecision.Reason.Unsupported,
                )
        }

    private fun apply(action: Entity.Action): EntityActionDecision =
        when (action) {
            is Brightness.SetBrightness ->
                EntityActionDecision.Accepted

            is OnOff.Toggle ->
                EntityActionDecision.Accepted

            else ->
                EntityActionDecision.Rejected(
                    reason = EntityActionDecision.Reason.Unsupported,
                )
        }
}
