package network.marsys.smarthome.hub.feature.entity.domain

import network.marsys.smarthome.domain.entity.Entity
import network.marsys.smarthome.hub.feature.entity.domain.action.PendingAction
import network.marsys.smarthome.hub.feature.entity.domain.event.EntityActionEvent
import network.marsys.smarthome.hub.feature.entity.domain.event.Event
import network.marsys.smarthome.hub.feature.entity.domain.reducer.EntityActionReducer
import network.marsys.smarthome.hub.feature.entity.domain.reducer.EntityReducer

class EntityAggregate(
    history: Collection<Event>,
) {
    val entity: Entity =
        EntityReducer.reduce(history = history)

    val pending: List<PendingAction> =
        EntityActionReducer.reduce(
            history = history
                .filterIsInstance<EntityActionEvent>(),
        )
}
