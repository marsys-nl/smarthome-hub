package network.marsys.smarthome.hub.feature.entity.application.reducer

import network.marsys.smarthome.domain.entity.EntityIdentifier
import network.marsys.smarthome.domain.entity.EntityNaming
import network.marsys.smarthome.domain.entity.Speaker
import network.marsys.smarthome.hub.feature.entity.domain.event.Event

internal object SpeakerReducer : EntityReducer<Speaker, Speaker.State, Speaker.State.Known, Speaker.State.Unknown>() {
    override fun createUnknownState() =
        Speaker.State.Unknown

    override fun reduce(
        identifier: EntityIdentifier,
        naming: EntityNaming,
        history: Collection<Event>,
    ): Speaker = history
        .fold(
            initial = Speaker(
                identifier = identifier,
                naming = naming,
                state = createUnknownState(),
            ),
            operation = { entity, event ->
                reduce<Speaker.State.Known>(entity = entity, event = event)
            },
        )

    override fun Speaker.update(state: Speaker.State): Speaker =
        copy(state = state)
}
