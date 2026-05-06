package network.marsys.smarthome.hub.plugin

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import network.marsys.smarthome.hub.plugin.auth.optionalApiKey

internal const val API_KEY_AUTH_NAME = "ApiKeyAuth"
internal const val API_KEY_CONFIG_KEY = "smarthome.auth.apiKey"

fun Application.initializeAuthentication() {
    val configuredApiKey = environment.config
        .propertyOrNull(API_KEY_CONFIG_KEY)
        ?.getString()
        ?.takeIf { it.isNotBlank() }

    install(Authentication) {
        optionalApiKey(
            name = API_KEY_AUTH_NAME,
            configuredApiKey = configuredApiKey,
        )
    }
}
