package network.marsys.smarthome.hub.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import network.marsys.smarthome.api.mappers.EntityCollectionApiMapper
import network.marsys.smarthome.hub.feature.entity.application.ports.inbound.GetEntities
import org.koin.ktor.ext.inject

fun Route.entityRoutes() {
    val getEntitiesUseCase by inject<GetEntities>()

    get("/api/entities") {
        call.respond(
            status = HttpStatusCode.OK,
            message = EntityCollectionApiMapper
                .invoke(source = getEntitiesUseCase.invoke()),
        )
    }
}
