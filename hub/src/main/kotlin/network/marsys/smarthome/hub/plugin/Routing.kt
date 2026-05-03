package network.marsys.smarthome.hub.plugin

import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.routing
import network.marsys.smarthome.hub.routes.configRoutes
import network.marsys.smarthome.hub.routes.healthRoutes

fun Application.initializeRouting() {
    routing {
        healthRoutes()

        authenticate(
            configurations = arrayOf(API_KEY_AUTH_NAME),
        ) {
            configRoutes()
        }
    }
}
