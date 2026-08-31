package network.marsys.smarthome.hub.feature.entity.application.reducer

import network.marsys.smarthome.domain.identifiers.EntityIdentifier
import network.marsys.smarthome.hub.feature.entity.domain.entity.Light
import network.marsys.smarthome.hub.feature.entity.domain.event.Event

internal object LightReducer : EntityReducer<Light, Light.State, Light.State.Known, Light.State.Unknown>() {
    override fun createUnknownState(lastKnown: Light.State.Known?) =
        Light.State.Unknown(lastKnown = lastKnown)

    override fun reduce(
        identifier: EntityIdentifier,
        history: Collection<Event>,
    ): Light = history
        .fold(
            initial = Light(
                identifier = identifier,
                state = createUnknownState(),
            ),
            operation = { entity, event ->
                reduce<Light.State.Known>(entity = entity, event = event)
            },
        )

    override fun Light.update(state: Light.State): Light =
        copy(state = state)
}
