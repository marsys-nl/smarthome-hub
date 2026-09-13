package network.marsys.smarthome.hub.routes

import de.infix.testBalloon.framework.core.testSuite
import dev.nmarsman.expect.api.expectThat
import dev.nmarsman.expect.assertions.isEqualTo
import dev.nmarsman.expect.assertions.single
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import network.marsys.smarthome.api.apiModuleSerializersModule
import network.marsys.smarthome.api.models.integration.IntegrationResponse
import network.marsys.smarthome.domain.identifiers.EntityIdentifier
import network.marsys.smarthome.domain.identifiers.IntegrationIdentifier
import network.marsys.smarthome.hub.feature.entity.application.usecase.GetEntities
import network.marsys.smarthome.hub.feature.entity.domain.entity.Entity
import network.marsys.smarthome.hub.feature.entity.domain.entity.Light
import network.marsys.smarthome.hub.feature.entity.domain.entity.System
import network.marsys.smarthome.hub.feature.integration.application.exception.IntegrationNotFoundException
import network.marsys.smarthome.hub.feature.integration.application.ports.inbound.IntegrationQueries
import network.marsys.smarthome.hub.feature.integration.application.ports.inbound.ManageIntegrationLifecycle
import network.marsys.smarthome.hub.feature.integration.domain.Integration
import network.marsys.smarthome.hub.feature.integration.infrastructure.FakeIntegrationAdapter
import network.marsys.smarthome.hub.plugin.initializeSerialization
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin

val EntityRoutesTest by testSuite(
    name = "Entity routes tests",
) {
    testSuite(
        name = "Getting entities",
    ) {
        test(name = "When no entities known the request returns empty list") {
            testApplication {
                initializeDependencyInjection(
                    entities = emptyList(),
                )

                application {
                    initializeSerialization()
                }

                routing {
                    entityRoutes()
                }

                val response = client.get("/api/entities")

                expectThat(response)
                    .get(HttpResponse::status)
                    .isEqualTo(HttpStatusCode.OK)

                expectThat(response.bodyAsText())
                    .isEqualTo("[]")
            }
        }

        test(name = "When an entities is known and has a unknown state the request returns that entity") {
            testApplication {
                initializeDependencyInjection(
                    entities = listOf(
                        Light(
                            identifier = EntityIdentifier("light.test"),
                            state = Light.State.Unknown(),
                        ),
                    ),
                )

                application {
                    initializeSerialization()
                }

                routing {
                    entityRoutes()
                }

                val response = client.get("/api/entities")

                expectThat(response)
                    .get(HttpResponse::status)
                    .isEqualTo(HttpStatusCode.OK)

                expectThat(response.bodyAsText())
                    .isEqualTo("""[{"type":"light","identifier":"light.test"}]""")
            }
        }

        test(name = "When entities are known the request returns those entities") {
            testApplication {
                initializeDependencyInjection(
                    entities = listOf(
                        Light(
                            identifier = EntityIdentifier("light.test"),
                            state = Light.State.Unknown(),
                        ),
                        System(
                            identifier = EntityIdentifier("system.smarthome"),
                            state = System.State.Unknown(),
                        ),
                    ),
                )

                application {
                    initializeSerialization()
                }

                routing {
                    entityRoutes()
                }

                val response = client.get("/api/entities")

                expectThat(response)
                    .get(HttpResponse::status)
                    .isEqualTo(HttpStatusCode.OK)

                expectThat(response.bodyAsText())
                    .isEqualTo("""[{"type":"light","identifier":"light.test"},{"type":"system","identifier":"system.smarthome"}]""")
            }
        }
    }
}

private fun ApplicationTestBuilder.initializeDependencyInjection(
    entities: List<Entity>,
) {
    install(Koin) {
        modules(
            module {
                single<GetEntities> {
                    GetEntities {
                        entities
                    }
                }
            },
        )
    }
}

private fun integrationQueries(
    integrations: Collection<Integration> = emptyList(),
) = object : IntegrationQueries {
    override fun all(): Collection<Integration> = integrations
}

private fun integrationLifecycleManager(
    start: (IntegrationIdentifier) -> Unit = {},
    stop: (IntegrationIdentifier) -> Unit = {},
    restart: (IntegrationIdentifier) -> Unit = {},
) = object : ManageIntegrationLifecycle {
    override suspend fun start(identifier: IntegrationIdentifier) = start.invoke(identifier)
    override suspend fun stop(identifier: IntegrationIdentifier) = stop.invoke(identifier)
    override suspend fun restart(identifier: IntegrationIdentifier) = restart.invoke(identifier)
}
