package network.marsys.smarthome.hub.plugin.auth

import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.AuthenticationConfig
import io.ktor.server.auth.AuthenticationContext
import io.ktor.server.auth.AuthenticationFailedCause
import io.ktor.server.auth.AuthenticationProvider
import io.ktor.server.auth.apikey.ApiKeyAuth
import io.ktor.server.response.respond

data object ApiKeyPrincipal

internal class OptionalApiKeyAuthProvider(
    private val configuration: Configuration,
) : AuthenticationProvider(configuration) {
    private val configuredApiKey: String? = configuration.configuredApiKey

    override suspend fun onAuthenticate(context: AuthenticationContext) {
        if (configuredApiKey == null) {
            context.principal(ApiKeyPrincipal)
            return
        }

        val apiKey = context.call.request.headers[ApiKeyAuth.DEFAULT_HEADER_NAME]
        val principal = apiKey
            ?.takeIf { it == configuredApiKey }
            ?.let { ApiKeyPrincipal }

        val cause = when {
            apiKey == null -> AuthenticationFailedCause.NoCredentials
            principal == null -> AuthenticationFailedCause.InvalidCredentials
            else -> null
        }

        cause?.also {
            context.challenge(
                key = configuration.key,
                cause = it,
            ) { challenge, call ->
                call.respond(HttpStatusCode.Unauthorized)
                challenge.complete()
            }
        }

        principal?.also {
            context.principal(principal)
        }
    }

    class Configuration internal constructor(
        internal val key: String,
        internal val configuredApiKey: String?,
    ) : Config(key)
}

fun AuthenticationConfig.optionalApiKey(
    name: String,
    configuredApiKey: String?,
) {
    val config = OptionalApiKeyAuthProvider.Configuration(name, configuredApiKey)
    val provider = OptionalApiKeyAuthProvider(config)
    register(provider)
}
