package network.marsys.smarthome.hub.feature.entity.domain.entity

import network.marsys.smarthome.domain.identifiers.EntityIdentifier
import network.marsys.smarthome.hub.feature.entity.domain.capability.Brightness
import network.marsys.smarthome.hub.feature.entity.domain.capability.Capability
import network.marsys.smarthome.hub.feature.entity.domain.capability.OnOff

data class Light(
    override val identifier: EntityIdentifier,
    override val state: State,
) : Entity<Light.State, Light.CapabilityDefinition> {
    sealed interface CapabilityDefinition : Entity.CapabilityDefinition {
        val onOff: Capability.Required<OnOff>
        val brightness: Capability.Optional<Brightness>
    }

    sealed interface State : Entity.State<CapabilityDefinition> {
        data class Known(
            override val onOff: Capability.Required<OnOff>,
            override val brightness: Capability.Optional<Brightness>,
        ) : CapabilityDefinition, State

        data class Unknown(
            override val lastKnown: CapabilityDefinition? = null,
        ) : State, Entity.State.Unknown<CapabilityDefinition>
    }

    companion object : Entity.Type<Light, CapabilityDefinition> {
        override fun create(identifier: EntityIdentifier): Light =
            Light(identifier, State.Unknown())
    }
}
