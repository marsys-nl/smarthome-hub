package network.marsys.smarthome.hub.feature.entity.application

import network.marsys.smarthome.hub.feature.entity.application.reducer.EntityReducer
import network.marsys.smarthome.hub.feature.entity.domain.entity.Entity
import network.marsys.smarthome.hub.feature.entity.domain.event.Event

internal class EntityAggregate(
    history: Collection<Event>,
) {
    val entity: Entity = EntityReducer.reduce(history = history)
}
