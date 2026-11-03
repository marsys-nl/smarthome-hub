package network.marsys.smarthome.hub.feature.entity.application.reducer

import network.marsys.smarthome.domain.entity.EntityIdentifier
import network.marsys.smarthome.domain.entity.EntityNaming
import network.marsys.smarthome.domain.entity.Light
import network.marsys.smarthome.hub.feature.entity.domain.event.Event

internal object LightReducer : EntityReducer<Light, Light.State, Light.State.Known, Light.State.Unknown>() {
    override fun createUnknownState() =
        Light.State.Unknown

    override fun reduce(
        identifier: EntityIdentifier,
        naming: EntityNaming,
        history: Collection<Event>,
    ): Light = history
        .fold(
            initial = Light(
                identifier = identifier,
                naming = naming,
                state = createUnknownState(),
            ),
            operation = { entity, event ->
                reduce<Light.State.Known>(entity = entity, event = event)
            },
        )

    override fun Light.update(state: Light.State): Light =
        copy(state = state)
}
