package network.marsys.smarthome.hub.feature.entity.application.reducer

import network.marsys.smarthome.domain.entity.EntityIdentifier
import network.marsys.smarthome.domain.entity.EntityNaming
import network.marsys.smarthome.domain.entity.Fan
import network.marsys.smarthome.hub.feature.entity.domain.event.Event

internal object FanReducer : EntityReducer<Fan, Fan.State, Fan.State.Known, Fan.State.Unknown>() {
    override fun createUnknownState() =
        Fan.State.Unknown

    override fun reduce(
        identifier: EntityIdentifier,
        naming: EntityNaming,
        history: Collection<Event>,
    ): Fan = history
        .fold(
            initial = Fan(
                identifier = identifier,
                naming = naming,
                state = createUnknownState(),
            ),
            operation = { entity, event ->
                reduce<Fan.State.Known>(entity = entity, event = event)
            },
        )

    override fun Fan.update(state: Fan.State): Fan =
        copy(state = state)
}
