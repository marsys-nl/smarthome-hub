package network.marsys.smarthome.hub.routes

import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import network.marsys.smarthome.api.models.config.HealthResponse

fun Route.healthRoutes() {
    get("/api/health") {
        call.respond(
            message = HealthResponse(
                app = "SmartHomeBackend",
                version = "2026.05",
            ),
        )
    }
}
