package network.marsys.smarthome.hub.feature.entity.domain.entity

import network.marsys.smarthome.domain.identifiers.EntityIdentifier
import network.marsys.smarthome.hub.feature.entity.domain.capability.Capability
import network.marsys.smarthome.hub.feature.entity.domain.capability.Duration

data class System(
    override val identifier: EntityIdentifier,
    override val state: State,
) : Entity {
    sealed interface State : Entity.State {
        data class Known(
            val uptime: Uptime,
        ) : State, Entity.State.Known {
            override fun updateWith(
                capability: Capability<*>,
            ): Entity.State = this
        }

        data class Unknown(
            override val lastKnown: Known? = null,
        ) : State, Entity.State.Unknown
    }

    data class Uptime(
        val host: Capability.Required<Duration>,
        val application: Capability.Required<Duration>,
    )

    companion object : Entity.Type<System>
}
