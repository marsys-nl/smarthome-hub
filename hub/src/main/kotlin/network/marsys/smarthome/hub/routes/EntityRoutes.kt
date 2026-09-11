package network.marsys.smarthome.hub.routes

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import network.marsys.smarthome.api.models.entity.EntityResponse
import network.marsys.smarthome.api.models.entity.LightEntity
import network.marsys.smarthome.api.models.entity.SystemEntity
import network.marsys.smarthome.hub.feature.entity.application.usecase.GetEntities
import network.marsys.smarthome.hub.feature.entity.domain.entity.Entity
import network.marsys.smarthome.hub.feature.entity.domain.entity.Light
import network.marsys.smarthome.hub.feature.entity.domain.entity.System
import org.koin.ktor.ext.inject

private val logger = KotlinLogging.logger { }

fun Route.entityRoutes() {
    val getEntitiesUseCase by inject<GetEntities>()

    get("/api/entities") {
        val entities = getEntitiesUseCase.invoke()
            .map(EntityResponse::map)

        call.respond(HttpStatusCode.OK, entities)
    }
}

private fun EntityResponse.Companion.map(entity: Entity): EntityResponse =
    when (entity) {
        is Light -> LightEntity(
            identifier = entity.identifier.value,
        )

        is System -> SystemEntity(
            identifier = entity.identifier.value,
        )
    }
