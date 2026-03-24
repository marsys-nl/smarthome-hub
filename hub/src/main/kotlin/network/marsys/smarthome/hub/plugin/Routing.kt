package network.marsys.smarthome.hub.plugin

import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import network.marsys.smarthome.hub.routes.configRoutes

suspend fun Application.initializeRouting() {
    routing {
        configRoutes()
    }
}
