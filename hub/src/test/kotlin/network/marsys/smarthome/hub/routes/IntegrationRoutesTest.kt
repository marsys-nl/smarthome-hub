package network.marsys.smarthome.hub.routes

import de.infix.testBalloon.framework.core.testSuite
import dev.nmarsman.expect.api.expectThat
import dev.nmarsman.expect.assertions.isEqualTo
import io.ktor.client.request.post
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import network.marsys.smarthome.domain.identifiers.IntegrationIdentifier
import network.marsys.smarthome.hub.feature.integration.application.IntegrationLifecycleManager
import network.marsys.smarthome.hub.feature.integration.application.exception.IntegrationNotFoundException
import network.marsys.smarthome.hub.feature.integration.application.ports.inbound.ManageIntegrationLifecycle
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin

val IntegrationRoutesTest by testSuite(
    name = "Integration routes tests",
) {
    testSuite(
        name = "Restarting integrations",
    ) {
        test(name = "When restarting known integration the request returns 200") {
            testApplication {
                initializeDependencyInjection()

                routing {
                    integrationRoutes()
                }

                val response = client.post("/api/integrations/valid-integration-identifier/restart")

                expectThat(response)
                    .get(HttpResponse::status)
                    .isEqualTo(HttpStatusCode.OK)
            }
        }

        test(name = "When restarting unknown integration the request returns 422") {
            testApplication {
                initializeDependencyInjection(
                    integrationLifecycleManager = integrationLifecycleManager(
                        restart = {
                            throw IntegrationNotFoundException(it)
                        },
                    ),
                )

                routing {
                    integrationRoutes()
                }

                val response = client.post("/api/integrations/unknown-itegration-identifier/restart")

                expectThat(response)
                    .get(HttpResponse::status)
                    .isEqualTo(HttpStatusCode.UnprocessableEntity)
            }
        }

        test(name = "When error happens on restarting valid integration the request returns 500") {
            testApplication {
                initializeDependencyInjection(
                    integrationLifecycleManager = integrationLifecycleManager(
                        restart = {
                            throw IllegalStateException("Some exception")
                        },
                    ),
                )

                routing {
                    integrationRoutes()
                }

                val response = client.post("/api/integrations/valid-integration-identifier/restart")

                expectThat(response)
                    .get(HttpResponse::status)
                    .isEqualTo(HttpStatusCode.InternalServerError)
            }
        }

        test(name = "When restarting invalid integration the request returns 400") {
            testApplication {
                initializeDependencyInjection()

                routing {
                    integrationRoutes()
                }

                val invalidIntegrationIdentifier = "a"
                val response = client.post("/api/integrations/${invalidIntegrationIdentifier}/restart")

                expectThat(response)
                    .get(HttpResponse::status)
                    .isEqualTo(HttpStatusCode.BadRequest)
            }
        }
    }

    testSuite(
        name = "Starting integrations",
    ) {
        test(name = "When starting known integration the request returns 200") {
            testApplication {
                initializeDependencyInjection()

                routing {
                    integrationRoutes()
                }

                val response = client.post("/api/integrations/valid-integration-identifier/start")

                expectThat(response)
                    .get(HttpResponse::status)
                    .isEqualTo(HttpStatusCode.OK)
            }
        }

        test(name = "When starting unknown integration the request returns 422") {
            testApplication {
                initializeDependencyInjection(
                    integrationLifecycleManager = integrationLifecycleManager(
                        start = {
                            throw IntegrationNotFoundException(it)
                        },
                    ),
                )

                routing {
                    integrationRoutes()
                }

                val response = client.post("/api/integrations/unknown-itegration-identifier/start")

                expectThat(response)
                    .get(HttpResponse::status)
                    .isEqualTo(HttpStatusCode.UnprocessableEntity)
            }
        }

        test(name = "When error happens on starting valid integration the request returns 500") {
            testApplication {
                initializeDependencyInjection(
                    integrationLifecycleManager = integrationLifecycleManager(
                        start = {
                            throw IllegalStateException("Some exception")
                        },
                    ),
                )

                routing {
                    integrationRoutes()
                }

                val response = client.post("/api/integrations/valid-integration-identifier/start")

                expectThat(response)
                    .get(HttpResponse::status)
                    .isEqualTo(HttpStatusCode.InternalServerError)
            }
        }

        test(name = "When starting invalid integration the request returns 400") {
            testApplication {
                initializeDependencyInjection()

                routing {
                    integrationRoutes()
                }

                val invalidIntegrationIdentifier = "a"
                val response = client.post("/api/integrations/${invalidIntegrationIdentifier}/start")

                expectThat(response)
                    .get(HttpResponse::status)
                    .isEqualTo(HttpStatusCode.BadRequest)
            }
        }
    }

    testSuite(
        name = "Stopping integrations",
    ) {
        test(name = "When stopping known integration the request gets accepted") {
            testApplication {
                initializeDependencyInjection()

                routing {
                    integrationRoutes()
                }

                val response = client.post("/api/integrations/valid-integration-identifier/stop")

                expectThat(response)
                    .get(HttpResponse::status)
                    .isEqualTo(HttpStatusCode.OK)
            }
        }

        test(name = "When stopping unknown integration the request returns 422") {
            testApplication {
                initializeDependencyInjection(
                    integrationLifecycleManager = integrationLifecycleManager(
                        stop = {
                            throw IntegrationNotFoundException(it)
                        },
                    ),
                )

                routing {
                    integrationRoutes()
                }

                val response = client.post("/api/integrations/unknown-itegration-identifier/stop")

                expectThat(response)
                    .get(HttpResponse::status)
                    .isEqualTo(HttpStatusCode.UnprocessableEntity)
            }
        }

        test(name = "When error happens on restarting valid integration the request returns 500") {
            testApplication {
                initializeDependencyInjection(
                    integrationLifecycleManager = integrationLifecycleManager(
                        stop = {
                            throw IllegalStateException("Some exception")
                        },
                    ),
                )

                routing {
                    integrationRoutes()
                }

                val response = client.post("/api/integrations/valid-integration-identifier/stop")

                expectThat(response)
                    .get(HttpResponse::status)
                    .isEqualTo(HttpStatusCode.InternalServerError)
            }
        }

        test(name = "When stopping invalid integration the request returns 400") {
            testApplication {
                initializeDependencyInjection()

                routing {
                    integrationRoutes()
                }

                val invalidIntegrationIdentifier = "a"
                val response = client.post("/api/integrations/${invalidIntegrationIdentifier}/stop")

                expectThat(response)
                    .get(HttpResponse::status)
                    .isEqualTo(HttpStatusCode.BadRequest)
            }
        }
    }
}

private fun ApplicationTestBuilder.initializeDependencyInjection(
    integrationLifecycleManager: ManageIntegrationLifecycle = integrationLifecycleManager(),
) {
    install(Koin) {
        modules(
            module {
                single<ManageIntegrationLifecycle> {
                    integrationLifecycleManager
                }
            },
        )
    }
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
