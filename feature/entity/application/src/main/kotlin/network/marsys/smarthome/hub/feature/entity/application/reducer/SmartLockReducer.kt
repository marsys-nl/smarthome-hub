package network.marsys.smarthome.hub.feature.entity.application.reducer

import network.marsys.smarthome.domain.entity.EntityIdentifier
import network.marsys.smarthome.domain.entity.EntityNaming
import network.marsys.smarthome.domain.entity.SmartLock
import network.marsys.smarthome.hub.feature.entity.domain.event.Event

internal object SmartLockReducer : EntityReducer<SmartLock, SmartLock.State, SmartLock.State.Known, SmartLock.State.Unknown>() {
    override fun createUnknownState() =
        SmartLock.State.Unknown

    override fun reduce(
        identifier: EntityIdentifier,
        naming: EntityNaming,
        history: Collection<Event>,
    ): SmartLock = history
        .fold(
            initial = SmartLock(
                identifier = identifier,
                naming = naming,
                state = createUnknownState(),
            ),
            operation = { entity, event ->
                reduce<SmartLock.State.Known>(entity = entity, event = event)
            },
        )

    override fun SmartLock.update(state: SmartLock.State): SmartLock =
        copy(state = state)
}
