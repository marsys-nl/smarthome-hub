package network.marsys.smarthome.hub.feature.entity.domain.entity

import network.marsys.smarthome.domain.identifiers.EntityIdentifier
import network.marsys.smarthome.hub.feature.entity.domain.capability.Brightness
import network.marsys.smarthome.hub.feature.entity.domain.capability.Capability
import network.marsys.smarthome.hub.feature.entity.domain.capability.Duration
import network.marsys.smarthome.hub.feature.entity.domain.capability.OnOff
import network.marsys.smarthome.hub.feature.entity.domain.capability.context.Application
import network.marsys.smarthome.hub.feature.entity.domain.capability.context.Host

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
            ): Entity.State = when (capability) {
                is Duration if Application in capability.context ->
                    copy(uptime = uptime.copy(application = uptime.application.updateWith(capability)))

                is Duration if Host in capability.context ->
                    copy(uptime = uptime.copy(host = uptime.host.updateWith(capability)))

                else -> unsupportedCapabilityError(capability = capability)
            }
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
