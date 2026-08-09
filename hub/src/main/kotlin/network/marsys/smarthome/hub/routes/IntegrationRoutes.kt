package network.marsys.smarthome.hub.routes

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingCall
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import network.marsys.smarthome.domain.identifiers.IntegrationIdentifier
import network.marsys.smarthome.hub.feature.integration.application.exception.IntegrationNotFoundException
import network.marsys.smarthome.hub.feature.integration.application.ports.inbound.ManageIntegrationLifecycle
import org.koin.ktor.ext.inject

private val logger = KotlinLogging.logger { }

@Suppress("LabeledExpression")
fun Route.integrationRoutes() {
    route("/api/integrations") {
        val manager by inject<ManageIntegrationLifecycle>()

        route("/{integrationIdentifier}") {
            post("/restart") {
                val identifier = extractIntegrationIdentifier(call)
                    ?: return@post call.respond(HttpStatusCode.BadRequest)

                executeIntegrationLifecycleAction {
                    manager.restart(identifier)
                }
            }

            post("/start") {
                val identifier = extractIntegrationIdentifier(call)
                    ?: return@post call.respond(HttpStatusCode.BadRequest)

                executeIntegrationLifecycleAction {
                    manager.start(identifier)
                }
            }

            post("/stop") {
                val identifier = extractIntegrationIdentifier(call)
                    ?: return@post call.respond(HttpStatusCode.BadRequest)

                executeIntegrationLifecycleAction {
                    manager.stop(identifier)
                }
            }
        }
    }
}

@Suppress("FunctionNameMaxLength")
private suspend fun RoutingContext.executeIntegrationLifecycleAction(
    action: suspend () -> Unit,
) = try {
    action.invoke()

    call.respond(HttpStatusCode.OK)
} catch (exception: IntegrationNotFoundException) {
    logger.debug(exception) { "Integration was not found." }
    call.respond(HttpStatusCode.UnprocessableEntity)
} catch (exception: IllegalStateException) {
    logger.debug(exception) { "Request failed to execute due to exception." }
    call.respond(HttpStatusCode.InternalServerError)
}

private fun extractIntegrationIdentifier(
    call: RoutingCall,
): IntegrationIdentifier? = try {
    call.parameters["integrationIdentifier"]!!
        .let(::IntegrationIdentifier)
} catch (exception: IllegalArgumentException) {
    logger.debug(exception) { "Error when trying to extract integration identifier." }
    null
}
