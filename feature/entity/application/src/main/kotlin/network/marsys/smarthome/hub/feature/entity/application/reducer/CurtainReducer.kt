package network.marsys.smarthome.hub.feature.entity.application.reducer

import network.marsys.smarthome.domain.entity.Curtain
import network.marsys.smarthome.domain.entity.EntityIdentifier
import network.marsys.smarthome.domain.entity.EntityNaming
import network.marsys.smarthome.hub.feature.entity.domain.event.Event

internal object CurtainReducer : EntityReducer<Curtain, Curtain.State, Curtain.State.Known, Curtain.State.Unknown>() {
    override fun createUnknownState() =
        Curtain.State.Unknown

    override fun reduce(
        identifier: EntityIdentifier,
        naming: EntityNaming,
        history: Collection<Event>,
    ): Curtain = history
        .fold(
            initial = Curtain(
                identifier = identifier,
                naming = naming,
                state = createUnknownState(),
            ),
            operation = { entity, event ->
                reduce<Curtain.State.Known>(entity = entity, event = event)
            },
        )

    override fun Curtain.update(state: Curtain.State): Curtain =
        copy(state = state)
}
