package network.marsys.smarthome.hub.feature.entity.application.usecase

import network.marsys.smarthome.hub.feature.entity.domain.entity.Entity

fun interface GetEntities {
    suspend operator fun invoke(): List<Entity>
}
