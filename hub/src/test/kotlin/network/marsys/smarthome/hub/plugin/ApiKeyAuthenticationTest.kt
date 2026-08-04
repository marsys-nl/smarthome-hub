package network.marsys.smarthome.hub.plugin

import de.infix.testBalloon.framework.core.testSuite
import dev.nmarsman.expect.api.expectThat
import dev.nmarsman.expect.api.expectThrows
import dev.nmarsman.expect.assertions.hasMessage
import dev.nmarsman.expect.assertions.isEqualTo
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.authenticate
import io.ktor.server.config.ApplicationConfigurationException
import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.testing.testApplication

val ApiKeyAuthenticationTest by testSuite(
    name = "Api key authentication tests",
) {
    test(name = "When an api key is configured and provided, the request is allowed") {
        testApplication {
            environment {
                config = MapApplicationConfig(
                    "smarthome.auth.apiKey" to "api-key",
                )
            }

            application {
                with(environment.config) {
                    install(Authentication) {
                        initializeApiKeyAuthentication()
                    }
                }
            }

            routing {
                authenticate(API_KEY_AUTH_NAME) {
                    get("/test") {
                        call.respond(HttpStatusCode.OK)
                    }
                }
            }

            val response = client.get("/test") {
                header("X-Api-Key", "api-key")
            }

            expectThat(response)
                .get(HttpResponse::status)
                .isEqualTo(HttpStatusCode.OK)
        }
    }

    test(name = "When an api key is configured but not provided, the request is disallowed") {
        testApplication {
            environment {
                config = MapApplicationConfig(
                    "smarthome.auth.apiKey" to "api-key",
                )
            }

            application {
                with(environment.config) {
                    install(Authentication) {
                        initializeApiKeyAuthentication()
                    }
                }
            }

            routing {
                authenticate(API_KEY_AUTH_NAME) {
                    get("/test") {
                        call.respond(HttpStatusCode.OK)
                    }
                }
            }

            val response = client.get("/test")

            expectThat(response)
                .get(HttpResponse::status)
                .isEqualTo(HttpStatusCode.Unauthorized)
        }
    }

    test(name = "When an api key is configured but another is provided, the request is disallowed") {
        testApplication {
            environment {
                config = MapApplicationConfig(
                    "smarthome.auth.apiKey" to "api-key",
                )
            }

            application {
                with(environment.config) {
                    install(Authentication) {
                        initializeApiKeyAuthentication()
                    }
                }
            }

            routing {
                authenticate(API_KEY_AUTH_NAME) {
                    get("/test") {
                        call.respond(HttpStatusCode.OK)
                    }
                }
            }

            val response = client.get("/test") {
                header("X-Api-Key", "other-api-key")
            }

            expectThat(response)
                .get(HttpResponse::status)
                .isEqualTo(HttpStatusCode.Unauthorized)
        }
    }

    test(name = "When no api key is configured and none provided, the request is allowed") {
        testApplication {
            application {
                with(environment.config) {
                    install(Authentication) {
                        initializeApiKeyAuthentication()
                    }
                }
            }

            routing {
                authenticate(API_KEY_AUTH_NAME) {
                    get("/test") {
                        call.respond(HttpStatusCode.OK)
                    }
                }
            }

            val response = client.get("/test")

            expectThat(response)
                .get(HttpResponse::status)
                .isEqualTo(HttpStatusCode.OK)
        }
    }

    test(name = "When no api key is configured but one is provided, the request is allowed") {
        testApplication {
            application {
                with(environment.config) {
                    install(Authentication) {
                        initializeApiKeyAuthentication()
                    }
                }
            }

            routing {
                authenticate(API_KEY_AUTH_NAME) {
                    get("/test") {
                        call.respond(HttpStatusCode.OK)
                    }
                }
            }

            val response = client.get("/test") {
                header("X-Api-Key", "api-key")
            }

            expectThat(response)
                .get(HttpResponse::status)
                .isEqualTo(HttpStatusCode.OK)
        }
    }

    test(name = "When an empty api key is configured, it is treated as not configured, the request is allowed") {
        testApplication {
            environment {
                config = MapApplicationConfig(
                    "smarthome.auth.apiKey" to "",
                )
            }

            application {
                with(environment.config) {
                    install(Authentication) {
                        initializeApiKeyAuthentication()
                    }
                }
            }

            routing {
                authenticate(API_KEY_AUTH_NAME) {
                    get("/test") {
                        call.respond(HttpStatusCode.OK)
                    }
                }
            }

            val response = client.get("/test")

            expectThat(response)
                .get(HttpResponse::status)
                .isEqualTo(HttpStatusCode.OK)
        }
    }
}
