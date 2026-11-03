package network.marsys.smarthome.hub.feature.entity.application.reducer

import network.marsys.smarthome.domain.entity.EntityIdentifier
import network.marsys.smarthome.domain.entity.EntityNaming
import network.marsys.smarthome.domain.entity.SmartPlug
import network.marsys.smarthome.hub.feature.entity.domain.event.Event

internal object SmartPlugReducer : EntityReducer<SmartPlug, SmartPlug.State, SmartPlug.State.Known, SmartPlug.State.Unknown>() {
    override fun createUnknownState() =
        SmartPlug.State.Unknown

    override fun reduce(
        identifier: EntityIdentifier,
        naming: EntityNaming,
        history: Collection<Event>,
    ): SmartPlug = history
        .fold(
            initial = SmartPlug(
                identifier = identifier,
                naming = naming,
                state = createUnknownState(),
            ),
            operation = { entity, event ->
                reduce<SmartPlug.State.Known>(entity = entity, event = event)
            },
        )

    override fun SmartPlug.update(state: SmartPlug.State): SmartPlug =
        copy(state = state)
}
