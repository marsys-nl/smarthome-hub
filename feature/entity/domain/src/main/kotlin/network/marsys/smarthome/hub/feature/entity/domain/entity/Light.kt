package network.marsys.smarthome.hub.feature.entity.domain.entity

import network.marsys.smarthome.domain.identifiers.EntityIdentifier
import network.marsys.smarthome.hub.feature.entity.domain.capability.Brightness
import network.marsys.smarthome.hub.feature.entity.domain.capability.Capability
import network.marsys.smarthome.hub.feature.entity.domain.capability.OnOff

data class Light(
    override val identifier: EntityIdentifier,
    override val state: State,
) : Entity<Light.State, Light.Capabilities> {
    sealed interface Capabilities : Entity.Capabilities {
        val onOff: Capability.Required<OnOff>
        val brightness: Capability.Optional<Brightness>
    }

    sealed interface State : Entity.State<Capabilities> {
        data class Known(
            override val onOff: Capability.Required<OnOff>,
            override val brightness: Capability.Optional<Brightness>,
        ) : Capabilities, State

        data class Unknown(
            override val lastKnown: Capabilities? = null,
        ) : State, Entity.State.Unknown<Capabilities>
    }

    companion object : Entity.Type<Light, Capabilities> {
        override fun createWithUnknown(identifier: EntityIdentifier): Light =
            Light(identifier, State.Unknown())
    }
}
