package network.marsys.smarthome.hub.plugin

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.apikey.ApiKeyAuth
import io.ktor.server.auth.apikey.apiKey
import io.ktor.server.config.ApplicationConfigurationException

internal const val API_KEY_AUTH_NAME = "ApiKeyAuth"
internal const val API_KEY_CONFIG_KEY = "smarthome.auth.apiKey"

data object ApiKeyPrincipal

fun Application.initializeAuthentication() {
    val configuredApiKey = environment.config
        .property(API_KEY_CONFIG_KEY)
        .getString()
        .takeIf { it.isNotBlank() }
        ?: throw ApplicationConfigurationException("Valid API key must be configured via '$API_KEY_CONFIG_KEY'.")

    install(Authentication) {
        apiKey(
            name = API_KEY_AUTH_NAME,
        ) {
            headerName = ApiKeyAuth.DEFAULT_HEADER_NAME

            validate { keyFromHeader ->
                keyFromHeader
                    .takeIf { it == configuredApiKey }
                    ?.let { ApiKeyPrincipal }
            }
        }
    }
}
