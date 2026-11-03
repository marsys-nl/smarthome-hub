package network.marsys.smarthome.hub.feature.entity.application.reducer

import network.marsys.smarthome.domain.entity.EntityIdentifier
import network.marsys.smarthome.domain.entity.EntityNaming
import network.marsys.smarthome.domain.entity.Shutter
import network.marsys.smarthome.hub.feature.entity.domain.event.Event

internal object ShutterReducer : EntityReducer<Shutter, Shutter.State, Shutter.State.Known, Shutter.State.Unknown>() {
    override fun createUnknownState() =
        Shutter.State.Unknown

    override fun reduce(
        identifier: EntityIdentifier,
        naming: EntityNaming,
        history: Collection<Event>,
    ): Shutter = history
        .fold(
            initial = Shutter(
                identifier = identifier,
                naming = naming,
                state = createUnknownState(),
            ),
            operation = { entity, event ->
                reduce<Shutter.State.Known>(entity = entity, event = event)
            },
        )

    override fun Shutter.update(state: Shutter.State): Shutter =
        copy(state = state)
}
