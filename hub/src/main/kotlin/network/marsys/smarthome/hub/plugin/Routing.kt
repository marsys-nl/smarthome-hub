package network.marsys.smarthome.hub.plugin

import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import network.marsys.smarthome.hub.routes.configRoutes
import network.marsys.smarthome.hub.routes.healthRoutes

fun Application.initializeRouting() {
    routing {
        authenticate(API_KEY_AUTH_NAME) {
            healthRoutes()
            configRoutes()
        }

        authenticate(BEARER_AUTH_NAME) {
            get("/auth-test") {
                call.respondText {
                    "Authenticated"
                }
            }
        }
    }
}
