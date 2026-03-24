package network.marsys.smarthome.hub.plugin

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.apikey.apiKey

internal const val API_KEY_AUTH_NAME = "ApiKeyAuth"
private const val API_KEY_HEADER_NAME = "X-API-Key"
private const val API_KEY_VALUE = "fbdf0cfb-d5a7-40ff-8464-5043a9ecea78"

data object ApiKeyPrincipal

fun Application.initializeAuthentication() {
    install(Authentication) {
        apiKey(
            name = API_KEY_AUTH_NAME,
        ) {
            headerName = API_KEY_HEADER_NAME

            validate { keyFromHeader ->
                keyFromHeader
                    .takeIf { it == API_KEY_VALUE }
                    ?.let { ApiKeyPrincipal }
            }
        }
    }
}
