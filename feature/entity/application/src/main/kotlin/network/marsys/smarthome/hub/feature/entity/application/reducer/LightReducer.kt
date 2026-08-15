package network.marsys.smarthome.hub.feature.entity.application.reducer

import network.marsys.smarthome.domain.identifiers.EntityIdentifier
import network.marsys.smarthome.hub.feature.entity.domain.entity.Light
import network.marsys.smarthome.hub.feature.entity.domain.event.EntityDiscovered
import network.marsys.smarthome.hub.feature.entity.domain.event.EntityProvisioned
import network.marsys.smarthome.hub.feature.entity.domain.event.Event

internal object LightReducer : EntityReducer<Light, Light.State, Light.Capabilities> {
    override fun createWithUnknownState(
        identifier: EntityIdentifier,
    ) = Light(
        identifier = identifier,
        state = Light.State.Unknown(),
    )

    override fun reduce(entity: Light, event: Event): Light {
        check(event !is EntityProvisioned) {
            "Entity has already been provisioned."
        }

        check(event.identifier == entity.identifier) {
            "Mismatched entity identifier '${event.identifier}' supplied while '${entity.identifier}' is expected."
        }

        return entity.copy(
            state = when (event) {
                is EntityDiscovered<*> -> handleEntityDiscovered(event = event)
                // else -> throwUnsupportedEventError(event = event)
            },
        )
    }

    private fun handleEntityDiscovered(event: EntityDiscovered<*>): Light.State.Known =
        checkNotNull(event.capabilities as? Light.State.Known) {
            "Entity capabilities of type '${event.capabilities::class.simpleName}' supplied " +
                "while 'Light.State.Known' is expected."
        }
}
