package network.marsys.smarthome.hub.feature.entity.application.reducer

import network.marsys.smarthome.domain.entity.EntityIdentifier
import network.marsys.smarthome.domain.entity.EntityNaming
import network.marsys.smarthome.domain.entity.System
import network.marsys.smarthome.hub.feature.entity.domain.event.Event

internal object SystemReducer : EntityReducer<System, System.State, System.State.Known, System.State.Unknown>() {
    override fun createUnknownState() =
        System.State.Unknown

    override fun reduce(
        identifier: EntityIdentifier,
        naming: EntityNaming,
        history: Collection<Event>,
    ): System = history
        .fold(
            initial = System(
                identifier = identifier,
                naming = naming,
                state = createUnknownState(),
            ),
            operation = { entity, event ->
                reduce<System.State.Known>(entity = entity, event = event)
            },
        )

    override fun System.update(state: System.State): System =
        copy(state = state)
}
