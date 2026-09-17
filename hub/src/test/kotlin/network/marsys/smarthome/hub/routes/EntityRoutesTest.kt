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
import kotlinx.serialization.json.JsonBuilder
import kotlinx.serialization.modules.SerializersModule
import network.marsys.smarthome.api.apiModuleSerializersModule
import network.marsys.smarthome.api.models.integration.IntegrationResponse
import network.marsys.smarthome.domain.identifiers.EntityIdentifier
import network.marsys.smarthome.domain.identifiers.IntegrationIdentifier
import network.marsys.smarthome.domain.unit.Dimension
import network.marsys.smarthome.domain.unit.Quantity
import network.marsys.smarthome.domain.unit.celsius
import network.marsys.smarthome.domain.unit.gibibytes
import network.marsys.smarthome.domain.unit.percent
import network.marsys.smarthome.hub.feature.entity.application.usecase.GetEntities
import network.marsys.smarthome.hub.feature.entity.domain.capability.Brightness
import network.marsys.smarthome.hub.feature.entity.domain.capability.Capability.Companion.optional
import network.marsys.smarthome.hub.feature.entity.domain.capability.Capability.Companion.required
import network.marsys.smarthome.hub.feature.entity.domain.capability.MeasuredDataSize
import network.marsys.smarthome.hub.feature.entity.domain.capability.MeasuredLoad
import network.marsys.smarthome.hub.feature.entity.domain.capability.MeasuredTemperature
import network.marsys.smarthome.hub.feature.entity.domain.capability.OnOff
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
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlin.time.toKotlinInstant

val EntityRoutesTest by testSuite(
    name = "Entity routes tests",
) {
    val builder: JsonBuilder.() -> Unit = {
        prettyPrint = true
    }

    testSuite(
        name = "Getting entities",
    ) {
        test(name = "When no entities known the request returns empty list") {
            testApplication {
                initializeDependencyInjection(
                    entities = emptyList(),
                )

                application {
                    initializeSerialization(builder = builder)
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

        test(name = "When an entity is known and has an unknown state the request returns that entity") {
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
                    initializeSerialization(builder = builder)
                }

                routing {
                    entityRoutes()
                }

                val response = client.get("/api/entities")

                expectThat(response)
                    .get(HttpResponse::status)
                    .isEqualTo(HttpStatusCode.OK)

                expectThat(response.bodyAsText())
                    .isEqualTo(
                        expected = """
                            |[
                            |    {
                            |        "type": "light",
                            |        "identifier": "light.test"
                            |    }
                            |]
                        """.trimMargin(),
                    )
            }
        }

        testSuite(
            name = "When an entity is known and has a known state the request returns that entity",
        ) {
            test(name = "Light entity - all capabilities known") {
                testApplication {
                    initializeDependencyInjection(
                        entities = listOf(
                            Light(
                                identifier = EntityIdentifier("light.test"),
                                state = Light.State.Known(
                                    onOff = required(OnOff(current = true)),
                                    brightness = optional(Brightness(current = 50.percent)),
                                ),
                            ),
                        ),
                    )

                    application {
                        initializeSerialization(builder = builder)
                    }

                    routing {
                        entityRoutes()
                    }

                    val response = client.get("/api/entities")

                    expectThat(response)
                        .get(HttpResponse::status)
                        .isEqualTo(HttpStatusCode.OK)

                    expectThat(response.bodyAsText())
                        .isEqualTo(
                            expected = """
                                |[
                                |    {
                                |        "type": "light",
                                |        "identifier": "light.test",
                                |        "state": {
                                |            "onOff": true,
                                |            "brightness": 50.0
                                |        }
                                |    }
                                |]
                            """.trimMargin(),
                        )
                }
            }

            test(name = "Light entity - only required capabilities known") {
                testApplication {
                    initializeDependencyInjection(
                        entities = listOf(
                            Light(
                                identifier = EntityIdentifier("light.test"),
                                state = Light.State.Known(
                                    onOff = required(OnOff(current = true)),
                                    brightness = optional(null),
                                ),
                            ),
                        ),
                    )

                    application {
                        initializeSerialization(builder = builder)
                    }

                    routing {
                        entityRoutes()
                    }

                    val response = client.get("/api/entities")

                    expectThat(response)
                        .get(HttpResponse::status)
                        .isEqualTo(HttpStatusCode.OK)

                    expectThat(response.bodyAsText())
                        .isEqualTo(
                            expected = """
                                |[
                                |    {
                                |        "type": "light",
                                |        "identifier": "light.test",
                                |        "state": {
                                |            "onOff": true
                                |        }
                                |    }
                                |]
                            """.trimMargin(),
                        )
                }
            }

            test(name = "System entity - all capabilities known") {
                testApplication {
                    initializeDependencyInjection(
                        entities = listOf(
                            System(
                                identifier = EntityIdentifier("system.test"),
                                state = System.State.Known(
                                    info = host(),
                                    processor = processor(),
                                    memory = memory(),
                                    uptime = uptime(),
                                ),
                            ),
                        ),
                    )

                    application {
                        initializeSerialization(builder = builder)
                    }

                    routing {
                        entityRoutes()
                    }

                    val response = client.get("/api/entities")

                    expectThat(response)
                        .get(HttpResponse::status)
                        .isEqualTo(HttpStatusCode.OK)

                    expectThat(response.bodyAsText())
                        .isEqualTo(
                            expected = """
                                |[
                                |    {
                                |        "type": "system",
                                |        "identifier": "system.test",
                                |        "state": {
                                |            "host": {
                                |                "device": {
                                |                    "manufacturer": "Raspberry Pi",
                                |                    "model": "4B",
                                |                    "architecture": "x64",
                                |                    "physicalCores": 1,
                                |                    "logicalCores": 4
                                |                },
                                |                "os": {
                                |                    "description": "Raspbian GNU/Linux 11 (bullseye)",
                                |                    "family": "Linux",
                                |                    "version": "11",
                                |                    "bitness": 64
                                |                }
                                |            },
                                |            "processor": {
                                |                "load": 5.0,
                                |                "temperature": 45.0
                                |            },
                                |            "memory": {
                                |                "total": 8.589934592E9,
                                |                "available": 4.294967296E9,
                                |                "swap": {
                                |                    "total": 2.147483648E9,
                                |                    "used": 1.073741824E9
                                |                }
                                |            },
                                |            "uptime": {
                                |                "host": "1970-01-01T00:00:00Z",
                                |                "application": "1970-01-01T01:00:00Z"
                                |            }
                                |        }
                                |    }
                                |]
                            """.trimMargin(),
                        )
                }
            }

            test(name = "System entity - only required capabilities known") {
                testApplication {
                    initializeDependencyInjection(
                        entities = listOf(
                            System(
                                identifier = EntityIdentifier("system.test"),
                                state = System.State.Known(
                                    info = host(),
                                    processor = processor(
                                        temperature = null,
                                    ),
                                    memory = memory(),
                                    uptime = uptime(),
                                ),
                            ),
                        ),
                    )

                    application {
                        initializeSerialization(builder = builder)
                    }

                    routing {
                        entityRoutes()
                    }

                    val response = client.get("/api/entities")

                    expectThat(response)
                        .get(HttpResponse::status)
                        .isEqualTo(HttpStatusCode.OK)

                    expectThat(response.bodyAsText())
                        .isEqualTo(
                            expected = """
                                |[
                                |    {
                                |        "type": "system",
                                |        "identifier": "system.test",
                                |        "state": {
                                |            "host": {
                                |                "device": {
                                |                    "manufacturer": "Raspberry Pi",
                                |                    "model": "4B",
                                |                    "architecture": "x64",
                                |                    "physicalCores": 1,
                                |                    "logicalCores": 4
                                |                },
                                |                "os": {
                                |                    "description": "Raspbian GNU/Linux 11 (bullseye)",
                                |                    "family": "Linux",
                                |                    "version": "11",
                                |                    "bitness": 64
                                |                }
                                |            },
                                |            "processor": {
                                |                "load": 5.0
                                |            },
                                |            "memory": {
                                |                "total": 8.589934592E9,
                                |                "available": 4.294967296E9,
                                |                "swap": {
                                |                    "total": 2.147483648E9,
                                |                    "used": 1.073741824E9
                                |                }
                                |            },
                                |            "uptime": {
                                |                "host": "1970-01-01T00:00:00Z",
                                |                "application": "1970-01-01T01:00:00Z"
                                |            }
                                |        }
                                |    }
                                |]
                            """.trimMargin(),
                        )
                }
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
                    initializeSerialization(builder = builder)
                }

                routing {
                    entityRoutes()
                }

                val response = client.get("/api/entities")

                expectThat(response)
                    .get(HttpResponse::status)
                    .isEqualTo(HttpStatusCode.OK)

                expectThat(response.bodyAsText())
                    .isEqualTo(
                        expected = """
                            |[
                            |    {
                            |        "type": "light",
                            |        "identifier": "light.test"
                            |    },
                            |    {
                            |        "type": "system",
                            |        "identifier": "system.smarthome"
                            |    }
                            |]
                        """.trimMargin(),
                    )
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

private fun host() = System.HostInfo(
    device = System.HostInfo.Device(
        manufacturer = "Raspberry Pi",
        model = "4B",
        architecture = "x64",
        physicalCores = 1,
        logicalCores = 4,
    ),
    operatingSystem = System.HostInfo.OperatingSystem(
        description = "Raspbian GNU/Linux 11 (bullseye)",
        family = "Linux",
        version = "11",
        bitness = 64,
    ),
)

private fun processor(
    load: Quantity<Dimension.Ratio> = 5.percent,
    temperature: Quantity<Dimension.Temperature>? = 45.celsius,
) = System.Processor(
    load = required(MeasuredLoad(current = load)),
    temperature = optional(temperature?.let { MeasuredTemperature(current = it) }),
)

private fun memory(
    total: Quantity<Dimension.DigitalInformation> = 8.gibibytes,
    available: Quantity<Dimension.DigitalInformation> = 4.gibibytes,
    swapTotal: Quantity<Dimension.DigitalInformation> = 2.gibibytes,
    swapUsed: Quantity<Dimension.DigitalInformation> = 1.gibibytes,
) = System.Memory(
    total = required(MeasuredDataSize(current = total)),
    available = required(MeasuredDataSize(current = available)),
    swap = System.Memory.Swap(
        total = required(MeasuredDataSize(current = swapTotal)),
        used = required(MeasuredDataSize(current = swapUsed)),
    ),
)

private fun uptime(
    host: Instant = Instant.fromEpochSeconds(0),
    application: Instant = Instant.fromEpochSeconds(3600),
) = System.Uptime(
    host = host,
    application = application,
)
