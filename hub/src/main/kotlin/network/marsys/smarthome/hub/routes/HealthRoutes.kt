package network.marsys.smarthome.hub.routes

import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import network.marsys.smarthome.api.models.config.HealthResponse
import network.marsys.smarthome.hub.BuildConfig

fun Route.healthRoutes() {
    get("/api/health") {
        call.respond(
            message = HealthResponse(
                app = "SmartHomeBackend",
                version = BuildConfig.VERSION,
            ),
        )
    }
}
