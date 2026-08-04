package network.marsys.smarthome.hub.plugin

import com.auth0.jwk.JwkProviderBuilder
import io.ktor.http.Url
import io.ktor.http.toURI
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.AuthenticationConfig
import io.ktor.server.auth.jwt.JWTAuthenticationProvider
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.config.ApplicationConfig
import network.marsys.smarthome.hub.plugin.auth.optionalApiKey
import java.util.concurrent.TimeUnit

internal const val API_KEY_AUTH_NAME = "ApiKeyAuth"
internal const val API_KEY_CONFIG_KEY = "smarthome.auth.apiKey"

internal const val BEARER_AUTH_NAME = "BearerAuth"
internal const val JWT_ISSUER_CONFIG_KEY = "smarthome.auth.jwt.issuer"
internal const val JWT_JWK_URL_CONFIG_KEY = "smarthome.auth.jwt.jwk-url"
internal const val JWT_REALM_CONFIG_KEY = "smarthome.auth.jwt.realm"

private const val JWK_CACHE_SIZE = 10L
private const val JWK_EXPIRES_HOURS = 24L
private const val JWK_REFILL_RATE = 1L

fun Application.initializeAuthentication() = with(environment.config) {
    install(Authentication) {
        initializeApiKeyAuthentication()
        initializeBearerAuthentication()
    }
}

context(config: ApplicationConfig)
internal fun AuthenticationConfig.initializeApiKeyAuthentication() {
    val configuredApiKey = config.propertyOrNull(API_KEY_CONFIG_KEY)?.getString()
    val formattedApiKey = configuredApiKey?.takeIf { it.isNotBlank() }

    optionalApiKey(
        name = API_KEY_AUTH_NAME,
        configuredApiKey = formattedApiKey,
    )
}

context(config: ApplicationConfig)
internal fun AuthenticationConfig.initializeBearerAuthentication(
    verifierConfigurer: JWTAuthenticationProvider.Config.(issuer: String, jwkUrl: String) -> Unit =
        defaultJwtVerifierConfigurer,
) {
    val configuredJwtIssuerUrl = config.property(JWT_ISSUER_CONFIG_KEY).getString()
    val configuredJwtJwkUrl = config.property(JWT_JWK_URL_CONFIG_KEY).getString()
    val configuredRealm = config.property(JWT_REALM_CONFIG_KEY).getString()

    jwt(name = BEARER_AUTH_NAME) {
        realm = configuredRealm

        verifierConfigurer.invoke(this, configuredJwtIssuerUrl, configuredJwtJwkUrl)

        validate { credential ->
            JWTPrincipal(credential.payload)
        }
    }
}

private val defaultJwtVerifierConfigurer: JWTAuthenticationProvider.Config.(String, String) -> Unit =
    { issuer, jwkUrl ->
        verifier(
            JwkProviderBuilder(Url(jwkUrl).toURI().toURL())
                .cached(JWK_CACHE_SIZE, JWK_EXPIRES_HOURS, TimeUnit.HOURS)
                .rateLimited(JWK_CACHE_SIZE, JWK_REFILL_RATE, TimeUnit.MINUTES)
                .build(),
            issuer,
        )
    }
