package network.marsys.smarthome.hub.feature.entity.domain.entity

import network.marsys.smarthome.domain.identifiers.EntityIdentifier
import network.marsys.smarthome.hub.feature.entity.domain.capability.Brightness
import network.marsys.smarthome.hub.feature.entity.domain.capability.Capability
import network.marsys.smarthome.hub.feature.entity.domain.capability.OnOff
import network.marsys.smarthome.hub.feature.entity.domain.event.Event

data class Light(
    override val identifier: EntityIdentifier,
    override val state: State,
) : Entity {
    sealed interface State : Entity.State {
        data class Known(
            val onOff: Capability.Required<OnOff>,
            val brightness: Capability.Optional<Brightness>,
        ) : State, Entity.State.Known

        data class Unknown(
            override val lastKnown: Known? = null,
        ) : State, Entity.State.Unknown
    }

    companion object : Entity.Type<Light>
}
