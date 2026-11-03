package network.marsys.smarthome.hub.feature.entity.application.reducer

import network.marsys.smarthome.domain.entity.Blind
import network.marsys.smarthome.domain.entity.EntityIdentifier
import network.marsys.smarthome.domain.entity.EntityNaming
import network.marsys.smarthome.hub.feature.entity.domain.event.Event

internal object BlindReducer : EntityReducer<Blind, Blind.State, Blind.State.Known, Blind.State.Unknown>() {
    override fun createUnknownState() =
        Blind.State.Unknown

    override fun reduce(
        identifier: EntityIdentifier,
        naming: EntityNaming,
        history: Collection<Event>,
    ): Blind = history
        .fold(
            initial = Blind(
                identifier = identifier,
                naming = naming,
                state = createUnknownState(),
            ),
            operation = { entity, event ->
                reduce<Blind.State.Known>(entity = entity, event = event)
            },
        )

    override fun Blind.update(state: Blind.State): Blind =
        copy(state = state)
}
