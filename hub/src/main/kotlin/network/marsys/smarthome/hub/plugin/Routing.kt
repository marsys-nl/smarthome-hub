package network.marsys.smarthome.hub.plugin

import io.ktor.server.application.Application
import io.ktor.server.auth.AuthenticationStrategy
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.routing
import network.marsys.smarthome.hub.routes.configRoutes
import network.marsys.smarthome.hub.routes.entityRoutes
import network.marsys.smarthome.hub.routes.healthRoutes
import network.marsys.smarthome.hub.routes.integrationRoutes

fun Application.initializeRouting() {
    routing {
        authenticate(API_KEY_AUTH_NAME) {
            healthRoutes()
            configRoutes()
        }

        authenticate(
            API_KEY_AUTH_NAME,
            BEARER_AUTH_NAME,
            strategy = AuthenticationStrategy.Required,
        ) {
            entityRoutes()
            integrationRoutes()
        }
    }
}
