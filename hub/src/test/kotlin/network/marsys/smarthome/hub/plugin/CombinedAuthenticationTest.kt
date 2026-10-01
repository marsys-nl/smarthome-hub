package network.marsys.smarthome.hub.plugin

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import de.infix.testBalloon.framework.core.testSuite
import dev.nmarsman.expect.api.expectThat
import dev.nmarsman.expect.assertions.isEqualTo
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.AuthenticationStrategy
import io.ktor.server.auth.authenticate
import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.testing.testApplication

val CombinedAuthenticationTest by testSuite(
    name = "Combined authentication tests",
) {
    test(name = "When an api key is configured and provided, and a valid bearer token provided, the request is allowed") {
        val algorithm = Algorithm.HMAC256("secret")

        val validToken = JWT.create()
            .withIssuer("https://auth.marsys.network/")
            .withSubject("test-user")
            .sign(algorithm)

        testApplication {
            environment {
                config = MapApplicationConfig(
                    "smarthome.auth.apiKey" to "api-key",
                    "smarthome.auth.jwt.realm" to "smarthome-app",
                    "smarthome.auth.jwt.issuer" to "https://auth.marsys.network/",
                    "smarthome.auth.jwt.jwk-url" to "https://auth.marsys.network/.well-known/jwks.json",
                )
            }

            application {
                with(environment.config) {
                    install(Authentication) {
                        initializeApiKeyAuthentication()
                        initializeBearerAuthentication(
                            verifierConfigurer = { issuer, _ ->
                                verifier(
                                    JWT
                                        .require(algorithm)
                                        .withIssuer(issuer)
                                        .build(),
                                )
                            },
                        )
                    }
                }
            }

            routing {
                authenticate(
                    API_KEY_AUTH_NAME,
                    BEARER_AUTH_NAME,
                    strategy = AuthenticationStrategy.Required,
                ) {
                    get("/test") {
                        call.respond(HttpStatusCode.OK)
                    }
                }
            }

            val response = client.get("/test") {
                header(HttpHeaders.Authorization, "Bearer $validToken")
                header("X-Api-Key", "api-key")
            }

            expectThat(response)
                .get(HttpResponse::status)
                .isEqualTo(HttpStatusCode.OK)
        }
    }

    test(name = "When no api key is configured or provided, and a valid bearer token provided, the request is allowed") {
        val algorithm = Algorithm.HMAC256("secret")

        val validToken = JWT.create()
            .withIssuer("https://auth.marsys.network/")
            .withSubject("test-user")
            .sign(algorithm)

        testApplication {
            environment {
                config = MapApplicationConfig(
                    "smarthome.auth.jwt.realm" to "smarthome-app",
                    "smarthome.auth.jwt.issuer" to "https://auth.marsys.network/",
                    "smarthome.auth.jwt.jwk-url" to "https://auth.marsys.network/.well-known/jwks.json",
                )
            }

            application {
                with(environment.config) {
                    install(Authentication) {
                        initializeApiKeyAuthentication()
                        initializeBearerAuthentication(
                            verifierConfigurer = { issuer, _ ->
                                verifier(
                                    JWT
                                        .require(algorithm)
                                        .withIssuer(issuer)
                                        .build(),
                                )
                            },
                        )
                    }
                }
            }

            routing {
                authenticate(
                    API_KEY_AUTH_NAME,
                    BEARER_AUTH_NAME,
                    strategy = AuthenticationStrategy.Required,
                ) {
                    get("/test") {
                        call.respond(HttpStatusCode.OK)
                    }
                }
            }

            val response = client.get("/test") {
                header(HttpHeaders.Authorization, "Bearer $validToken")
            }

            expectThat(response)
                .get(HttpResponse::status)
                .isEqualTo(HttpStatusCode.OK)
        }
    }

    test(name = "When an api key is configured but not provided, and a valid bearer token provided, the request is disallowed") {
        val algorithm = Algorithm.HMAC256("secret")

        val validToken = JWT.create()
            .withIssuer("https://auth.marsys.network/")
            .withSubject("test-user")
            .sign(algorithm)

        testApplication {
            environment {
                config = MapApplicationConfig(
                    "smarthome.auth.apiKey" to "api-key",
                    "smarthome.auth.jwt.realm" to "smarthome-app",
                    "smarthome.auth.jwt.issuer" to "https://auth.marsys.network/",
                    "smarthome.auth.jwt.jwk-url" to "https://auth.marsys.network/.well-known/jwks.json",
                )
            }

            application {
                with(environment.config) {
                    install(Authentication) {
                        initializeApiKeyAuthentication()
                        initializeBearerAuthentication(
                            verifierConfigurer = { issuer, _ ->
                                verifier(
                                    JWT
                                        .require(algorithm)
                                        .withIssuer(issuer)
                                        .build(),
                                )
                            },
                        )
                    }
                }
            }

            routing {
                authenticate(
                    API_KEY_AUTH_NAME,
                    BEARER_AUTH_NAME,
                    strategy = AuthenticationStrategy.Required,
                ) {
                    get("/test") {
                        call.respond(HttpStatusCode.OK)
                    }
                }
            }

            val response = client.get("/test") {
                header(HttpHeaders.Authorization, "Bearer $validToken")
            }

            expectThat(response)
                .get(HttpResponse::status)
                .isEqualTo(HttpStatusCode.Unauthorized)
        }
    }

    test(name = "When an api key is configured and provided, and an invalid bearer token provided, the request is disallowed") {
        val algorithm = Algorithm.HMAC256("secret")

        val validToken = JWT.create()
            .withIssuer("https://auth.marsys.network/")
            .withSubject("test-user")
            .sign(algorithm)

        testApplication {
            environment {
                config = MapApplicationConfig(
                    "smarthome.auth.apiKey" to "api-key",
                    "smarthome.auth.jwt.realm" to "smarthome-app",
                    "smarthome.auth.jwt.issuer" to "https://auth.marsys.network/",
                    "smarthome.auth.jwt.jwk-url" to "https://auth.marsys.network/.well-known/jwks.json",
                )
            }

            application {
                with(environment.config) {
                    install(Authentication) {
                        initializeApiKeyAuthentication()
                        initializeBearerAuthentication(
                            verifierConfigurer = { issuer, _ ->
                                verifier(
                                    JWT
                                        .require(algorithm)
                                        .withIssuer(issuer)
                                        .build(),
                                )
                            },
                        )
                    }
                }
            }

            routing {
                authenticate(
                    API_KEY_AUTH_NAME,
                    BEARER_AUTH_NAME,
                    strategy = AuthenticationStrategy.Required,
                ) {
                    get("/test") {
                        call.respond(HttpStatusCode.OK)
                    }
                }
            }

            val response = client.get("/test") {
                header(HttpHeaders.Authorization, "Bearer invalid-token")
                header("X-Api-Key", "api-key")
            }

            expectThat(response)
                .get(HttpResponse::status)
                .isEqualTo(HttpStatusCode.Unauthorized)
        }
    }
}
