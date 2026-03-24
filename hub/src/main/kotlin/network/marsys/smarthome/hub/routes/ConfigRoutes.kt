package network.marsys.smarthome.hub.routes

import io.ktor.http.URLProtocol
import io.ktor.server.plugins.origin
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import network.marsys.smarthome.api.models.config.ConfigurationResponse

fun Route.configRoutes() {
    get("/api/config") {
        val origin = call.request.origin

        val defaultPort = when (origin.scheme) {
            URLProtocol.HTTP.name -> URLProtocol.HTTP.defaultPort
            URLProtocol.HTTPS.name -> URLProtocol.HTTPS.defaultPort
            else -> -1
        }

        val portPart = when {
            origin.serverPort == defaultPort -> ""
            origin.serverPort <= 0 -> ""
            else -> ":${origin.serverPort}"
        }

        val response = ConfigurationResponse(
            baseUri = "${origin.scheme}://${origin.serverHost}$portPart",
        )

        call.respond(response)
    }
}
