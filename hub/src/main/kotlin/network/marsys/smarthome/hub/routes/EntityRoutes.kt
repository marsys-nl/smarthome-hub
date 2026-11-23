package network.marsys.smarthome.hub.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import network.marsys.smarthome.api.mappers.EntityCollectionApiMapper
import network.marsys.smarthome.api.mappers.entity.action.EntityActionDomainMapper
import network.marsys.smarthome.api.models.entity.action.EntityActionRequest
import network.marsys.smarthome.hub.feature.entity.application.ports.inbound.ExecuteEntityAction
import network.marsys.smarthome.hub.feature.entity.application.ports.inbound.GetEntities
import network.marsys.smarthome.hub.feature.entity.domain.EntityActionDecision.Reason
import org.koin.ktor.ext.inject

fun Route.entityRoutes() {
    val executeEntityAction by inject<ExecuteEntityAction>()
    val getEntitiesUseCase by inject<GetEntities>()

    get("/api/entities") {
        call.respond(
            status = HttpStatusCode.OK,
            message = EntityCollectionApiMapper
                .invoke(source = getEntitiesUseCase.invoke()),
        )
    }

    post("/api/entities/actions") {
        try {
            val request = call.receive<EntityActionRequest>()
            val action = EntityActionDomainMapper.invoke(source = request)

            when (val result = executeEntityAction.invoke(action = action)) {
                ExecuteEntityAction.Result.Accepted ->
                    call.respond(HttpStatusCode.Accepted)

                ExecuteEntityAction.Result.NotFound ->
                    call.respond(HttpStatusCode.NotFound)

                is ExecuteEntityAction.Result.Rejected ->
                    when (result.reason) {
                        is Reason.DuplicateAction ->
                            call.respond(HttpStatusCode.Conflict)

                        is Reason.DuplicateState ->
                            call.respond(HttpStatusCode.NotAcceptable)

                        is Reason.MismatchedEntity ->
                            call.respond(HttpStatusCode.NotFound)

                        is Reason.Unsupported ->
                            call.respond(HttpStatusCode.UnprocessableEntity)
                    }
            }
        } catch (_: Exception) {
            call.respond(HttpStatusCode.BadRequest)
        }
    }
}
