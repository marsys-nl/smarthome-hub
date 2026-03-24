package network.marsys.smarthome.hub.plugin

import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.routing
import network.marsys.smarthome.hub.routes.configRoutes

fun Application.initializeRouting() {
    routing {
        authenticate(
            configurations = arrayOf(API_KEY_AUTH_NAME),
        ) {
            configRoutes()
        }
    }
}
