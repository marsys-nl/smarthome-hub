package network.marsys.smarthome.hub.feature.entity.application.ports.outbound

import network.marsys.smarthome.hub.feature.entity.domain.event.EntityActionAccepted

fun interface EntityActionDispatcher {
    suspend fun dispatch(accepted: EntityActionAccepted)
}
