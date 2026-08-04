package network.marsys.smarthome.hub.routes

import de.infix.testBalloon.framework.core.testSuite
import dev.nmarsman.expect.api.expectThat
import dev.nmarsman.expect.assertions.isEqualTo
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.testApplication
import network.marsys.smarthome.api.models.config.ConfigurationResponse
import network.marsys.smarthome.hub.plugin.initializeForwardedHeaders
import network.marsys.smarthome.hub.plugin.initializeSerialization

val ConfigRoutesTest by testSuite(
    name = "Config routes tests",
) {
    test(name = "When no explicit scheme, uri or port supplied the base uri returns localhost") {
        testApplication {
            application {
                initializeForwardedHeaders()
                initializeSerialization()
            }

            routing {
                configRoutes()
            }

            val client = createClient {
                install(ContentNegotiation) {
                    json()
                }
            }

            val response = client.get("/api/config")

            expectThat(response)
                .get(HttpResponse::status)
                .isEqualTo(HttpStatusCode.OK)

            expectThat(response.body<ConfigurationResponse>())
                .get(ConfigurationResponse::baseUri)
                .isEqualTo("http://localhost")
        }
    }

    test(name = "When `https://` scheme is supplied the base uri contains `https://`") {
        testApplication {
            application {
                initializeForwardedHeaders()
                initializeSerialization()
            }

            routing {
                configRoutes()
            }

            val client = createClient {
                install(ContentNegotiation) {
                    json()
                }
            }

            val response = client.get("/api/config") {
                header(HttpHeaders.XForwardedProto, "https")
            }

            expectThat(response)
                .get(HttpResponse::status)
                .isEqualTo(HttpStatusCode.OK)

            expectThat(response.body<ConfigurationResponse>())
                .get(ConfigurationResponse::baseUri)
                .isEqualTo("https://localhost")
        }
    }

    test(name = "When a port is supplied the base uri doesn't contain the port if it's a default port - 80") {
        testApplication {
            application {
                initializeForwardedHeaders()
                initializeSerialization()
            }

            routing {
                configRoutes()
            }

            val client = createClient {
                install(ContentNegotiation) {
                    json()
                }
            }

            val response = client.get("/api/config") {
                header(HttpHeaders.XForwardedPort, 80)
            }

            expectThat(response)
                .get(HttpResponse::status)
                .isEqualTo(HttpStatusCode.OK)

            expectThat(response.body<ConfigurationResponse>())
                .get(ConfigurationResponse::baseUri)
                .isEqualTo("http://localhost")
        }
    }

    test(name = "When a port is supplied the base uri doesn't contain the port if it's a default port - 443") {
        testApplication {
            application {
                initializeForwardedHeaders()
                initializeSerialization()
            }

            routing {
                configRoutes()
            }

            val client = createClient {
                install(ContentNegotiation) {
                    json()
                }
            }

            val response = client.get("/api/config") {
                header(HttpHeaders.XForwardedProto, "https")
                header(HttpHeaders.XForwardedPort, 443)
            }

            expectThat(response)
                .get(HttpResponse::status)
                .isEqualTo(HttpStatusCode.OK)

            expectThat(response.body<ConfigurationResponse>())
                .get(ConfigurationResponse::baseUri)
                .isEqualTo("https://localhost")
        }
    }

    test(name = "When no port is supplied the base uri doesn't contain the port") {
        testApplication {
            application {
                initializeForwardedHeaders()
                initializeSerialization()
            }

            routing {
                configRoutes()
            }

            val client = createClient {
                install(ContentNegotiation) {
                    json()
                }
            }

            val response = client.get("/api/config") {
                header(HttpHeaders.XForwardedProto, "https")
                header(HttpHeaders.XForwardedPort, 0)
            }

            expectThat(response)
                .get(HttpResponse::status)
                .isEqualTo(HttpStatusCode.OK)

            expectThat(response.body<ConfigurationResponse>())
                .get(ConfigurationResponse::baseUri)
                .isEqualTo("https://localhost")
        }
    }

    test(name = "When a port is supplied the base uri contains the port if it isn't a default port") {
        testApplication {
            application {
                initializeForwardedHeaders()
                initializeSerialization()
            }

            routing {
                configRoutes()
            }

            val client = createClient {
                install(ContentNegotiation) {
                    json()
                }
            }

            val response = client.get("/api/config") {
                header(HttpHeaders.XForwardedProto, "https")
                header(HttpHeaders.XForwardedPort, 8443)
            }

            expectThat(response)
                .get(HttpResponse::status)
                .isEqualTo(HttpStatusCode.OK)

            expectThat(response.body<ConfigurationResponse>())
                .get(ConfigurationResponse::baseUri)
                .isEqualTo("https://localhost:8443")
        }
    }

    test(name = "When a non-http scheme is supplied the base uri contains the port") {
        testApplication {
            application {
                initializeForwardedHeaders()
                initializeSerialization()
            }

            routing {
                configRoutes()
            }

            val client = createClient {
                install(ContentNegotiation) {
                    json()
                }
            }

            val response = client.get("/api/config") {
                header(HttpHeaders.XForwardedProto, "ftp")
                header(HttpHeaders.XForwardedPort, 8443)
            }

            expectThat(response)
                .get(HttpResponse::status)
                .isEqualTo(HttpStatusCode.OK)

            expectThat(response.body<ConfigurationResponse>())
                .get(ConfigurationResponse::baseUri)
                .isEqualTo("ftp://localhost:8443")
        }
    }

    test(name = "When a domain is provided the base uri contains the domain") {
        testApplication {
            application {
                initializeForwardedHeaders()
                initializeSerialization()
            }

            routing {
                configRoutes()
            }

            val client = createClient {
                install(ContentNegotiation) {
                    json()
                }
            }

            val response = client.get("/api/config") {
                header(HttpHeaders.XForwardedHost, "smarthome.marsys.network")
            }

            expectThat(response)
                .get(HttpResponse::status)
                .isEqualTo(HttpStatusCode.OK)

            expectThat(response.body<ConfigurationResponse>())
                .get(ConfigurationResponse::baseUri)
                .isEqualTo("http://smarthome.marsys.network")
        }
    }
}
