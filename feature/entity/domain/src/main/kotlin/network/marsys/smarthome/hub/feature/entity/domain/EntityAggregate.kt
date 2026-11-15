package network.marsys.smarthome.hub.feature.entity.domain

import network.marsys.smarthome.domain.entity.Entity
import network.marsys.smarthome.hub.feature.entity.domain.event.Event
import network.marsys.smarthome.hub.feature.entity.domain.reducer.EntityReducer

class EntityAggregate(
    history: Collection<Event>,
) {
    val entity: Entity = EntityReducer.reduce(history = history)
}
