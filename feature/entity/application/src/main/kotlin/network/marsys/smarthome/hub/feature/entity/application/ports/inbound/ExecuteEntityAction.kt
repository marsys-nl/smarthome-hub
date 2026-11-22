package network.marsys.smarthome.hub.feature.entity.application.ports.inbound

import network.marsys.smarthome.domain.entity.Entity

fun interface ExecuteEntityAction {
    suspend operator fun invoke(action: Entity.Action)
}
