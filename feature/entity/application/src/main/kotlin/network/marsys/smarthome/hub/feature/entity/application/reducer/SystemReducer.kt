package network.marsys.smarthome.hub.feature.entity.application.reducer

import network.marsys.smarthome.domain.identifiers.EntityIdentifier
import network.marsys.smarthome.hub.feature.entity.domain.entity.System
import network.marsys.smarthome.hub.feature.entity.domain.event.Event

internal object SystemReducer : EntityReducer<System, System.State, System.State.Known, System.State.Unknown>() {
    override fun createUnknownState(lastKnown: System.State.Known?) =
        System.State.Unknown(lastKnown = lastKnown)

    override fun reduce(
        identifier: EntityIdentifier,
        history: Collection<Event>,
    ): System = history
        .fold(
            initial = System(
                identifier = identifier,
                state = createUnknownState(),
            ),
            operation = { entity, event ->
                reduce<System.State.Known>(entity = entity, event = event)
            },
        )

    override fun System.update(state: System.State): System =
        copy(state = state)
}
