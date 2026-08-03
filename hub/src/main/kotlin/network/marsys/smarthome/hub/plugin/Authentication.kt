package network.marsys.smarthome.hub.plugin

import com.auth0.jwk.JwkProviderBuilder
import io.ktor.http.Url
import io.ktor.http.toURI
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.AuthenticationConfig
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.config.ApplicationConfig
import network.marsys.smarthome.hub.plugin.auth.optionalApiKey
import java.util.concurrent.TimeUnit

internal const val API_KEY_AUTH_NAME = "ApiKeyAuth"
internal const val API_KEY_CONFIG_KEY = "smarthome.auth.apiKey"

internal const val KEYCLOAK_AUTH_NAME = "KeycloakAuth"
internal const val KEYCLOAK_ISSUER_CONFIG_KEY = "smarthome.auth.keycloak.issuer"
internal const val KEYCLOAK_JWK_CONFIG_KEY = "smarthome.auth.keycloak.jwk"

fun Application.initializeAuthentication() = with(environment.config) {
    install(Authentication) {
        initializeApiKeyAuthentication()
        initializeBearerTokenAuthentication()
    }
}

context(config: ApplicationConfig)
private fun AuthenticationConfig.initializeApiKeyAuthentication() {
    val configuredApiKey = config.propertyOrNull(API_KEY_CONFIG_KEY)
        ?.getString()
        ?.takeIf { it.isNotBlank() }

    optionalApiKey(
        name = API_KEY_AUTH_NAME,
        configuredApiKey = configuredApiKey,
    )
}

context(config: ApplicationConfig)
private fun AuthenticationConfig.initializeBearerTokenAuthentication() {
    val keycloakIssuerUrl = config.property(KEYCLOAK_ISSUER_CONFIG_KEY).getString()
    val keycloakJwkUrl = config.property(KEYCLOAK_JWK_CONFIG_KEY).getString()

    jwt(name = KEYCLOAK_AUTH_NAME) {
        realm = "smarthome-app"

        verifier(
            JwkProviderBuilder(Url(keycloakJwkUrl).toURI().toURL())
                .cached(10, 24, TimeUnit.HOURS)
                .rateLimited(10, 1, TimeUnit.MINUTES)
                .build(),
            keycloakIssuerUrl,
        )

        validate { credential ->
            credential.payload
                .takeIf { it.expiresAt.time > System.currentTimeMillis() }
                ?.let(::JWTPrincipal)
        }
    }
}
