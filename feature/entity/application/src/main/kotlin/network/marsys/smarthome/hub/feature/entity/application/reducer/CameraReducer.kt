package network.marsys.smarthome.hub.feature.entity.application.reducer

import network.marsys.smarthome.domain.entity.Camera
import network.marsys.smarthome.domain.entity.EntityIdentifier
import network.marsys.smarthome.domain.entity.EntityNaming
import network.marsys.smarthome.hub.feature.entity.domain.event.Event

internal object CameraReducer : EntityReducer<Camera, Camera.State, Camera.State.Known, Camera.State.Unknown>() {
    override fun createUnknownState() =
        Camera.State.Unknown

    override fun reduce(
        identifier: EntityIdentifier,
        naming: EntityNaming,
        history: Collection<Event>,
    ): Camera = history
        .fold(
            initial = Camera(
                identifier = identifier,
                naming = naming,
                state = createUnknownState(),
            ),
            operation = { entity, event ->
                reduce<Camera.State.Known>(entity = entity, event = event)
            },
        )

    override fun Camera.update(state: Camera.State): Camera =
        copy(state = state)
}
