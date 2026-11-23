package network.marsys.smarthome.hub.routes

import de.infix.testBalloon.framework.core.testSuite
import dev.nmarsman.expect.api.expectThat
import dev.nmarsman.expect.assertions.isEqualTo
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonBuilder
import kotlinx.serialization.modules.SerializersModule
import network.marsys.smarthome.api.apiModuleSerializersModule
import network.marsys.smarthome.api.mappers.entity.action.EntityActionApiMapper
import network.marsys.smarthome.api.models.entity.action.EntityActionRequest
import network.marsys.smarthome.domain.TranslationKey
import network.marsys.smarthome.domain.capability.Brightness
import network.marsys.smarthome.domain.capability.Capability.Companion.optional
import network.marsys.smarthome.domain.capability.Capability.Companion.required
import network.marsys.smarthome.domain.capability.MeasureDataSize
import network.marsys.smarthome.domain.capability.MeasureLoad
import network.marsys.smarthome.domain.capability.MeasureTemperature
import network.marsys.smarthome.domain.capability.OnOff
import network.marsys.smarthome.domain.entity.Entity
import network.marsys.smarthome.domain.entity.EntityIdentifier
import network.marsys.smarthome.domain.entity.Light
import network.marsys.smarthome.domain.entity.System
import network.marsys.smarthome.domain.entity.naming
import network.marsys.smarthome.domain.unit.Dimension
import network.marsys.smarthome.domain.unit.Quantity
import network.marsys.smarthome.domain.unit.celsius
import network.marsys.smarthome.domain.unit.gibibytes
import network.marsys.smarthome.domain.unit.percent
import network.marsys.smarthome.hub.feature.entity.application.ports.inbound.ExecuteEntityAction
import network.marsys.smarthome.hub.feature.entity.application.ports.inbound.GetEntities
import network.marsys.smarthome.hub.feature.entity.domain.EntityActionDecision
import network.marsys.smarthome.hub.plugin.applicationModule
import network.marsys.smarthome.hub.plugin.initializeSerialization
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import kotlin.time.Instant

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
                            naming = Light.naming(),
                            state = Light.State.Unknown,
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
                            |        "naming": {
                            |            "key": "light"
                            |        }
                            |    }
                            |]
                        """.trimMargin(),
                    )
            }
        }

        test(name = "When an entity is known and has a naming qualifier the request returns that entity") {
            testApplication {
                initializeDependencyInjection(
                    entities = listOf(
                        Light(
                            identifier = EntityIdentifier("light.test"),
                            naming = Light.naming(
                                qualifier = TranslationKey("ceiling"),
                            ),
                            state = Light.State.Unknown,
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
                            |        "naming": {
                            |            "key": "light",
                            |            "qualifier": "ceiling"
                            |        }
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
                                naming = Light.naming(),
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
                                |        "naming": {
                                |            "key": "light"
                                |        },
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
                                naming = Light.naming(),
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
                                |        "naming": {
                                |            "key": "light"
                                |        },
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
                                naming = System.naming(),
                                state = System.State.Known(
                                    host = host(),
                                    processor = processor(),
                                    memory = memory(),
                                    startedAt = uptime(),
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
                                |        "naming": {
                                |            "key": "system"
                                |        },
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
                                |                    "available": 1.073741824E9
                                |                }
                                |            },
                                |            "startedAt": {
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
                                naming = System.naming(),
                                state = System.State.Known(
                                    host = host(),
                                    processor = processor(
                                        temperature = null,
                                    ),
                                    memory = memory(),
                                    startedAt = uptime(),
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
                                |        "naming": {
                                |            "key": "system"
                                |        },
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
                                |                    "available": 1.073741824E9
                                |                }
                                |            },
                                |            "startedAt": {
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
                            naming = Light.naming(),
                            state = Light.State.Unknown,
                        ),
                        System(
                            identifier = EntityIdentifier("system.smarthome"),
                            naming = System.naming(),
                            state = System.State.Unknown,
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
                            |        "naming": {
                            |            "key": "light"
                            |        }
                            |    },
                            |    {
                            |        "type": "system",
                            |        "identifier": "system.smarthome",
                            |        "naming": {
                            |            "key": "system"
                            |        }
                            |    }
                            |]
                        """.trimMargin(),
                    )
            }
        }
    }

    testSuite(
        name = "Executing entity actions",
    ) {
        test(name = "When an entity action is accepted the request returns http status accepted") {
            testApplication {
                initializeDependencyInjection(
                    executeEntityAction = {
                        ExecuteEntityAction.Result.Accepted
                    },
                )

                application {
                    initializeSerialization(builder = builder)
                }

                routing {
                    entityRoutes()
                }

                val client = createClient {
                    install(ContentNegotiation) {
                        json(
                            Json {
                                serializersModule = SerializersModule {
                                    include(apiModuleSerializersModule)
                                }
                            },
                        )
                    }
                }

                val response = client.post("/api/entities/actions") {
                    contentType(ContentType.Application.Json)
                    setBody<EntityActionRequest?>(
                        body = EntityActionApiMapper.invoke(
                            source = OnOff.Toggle.On(
                                entity = EntityIdentifier("light.test"),
                            ),
                        ),
                    )
                }

                expectThat(response)
                    .get(HttpResponse::status)
                    .isEqualTo(HttpStatusCode.Accepted)
            }
        }

        test(name = "When an entity identifier is not found the request returns http status not found") {
            testApplication {
                initializeDependencyInjection(
                    executeEntityAction = {
                        ExecuteEntityAction.Result.NotFound
                    },
                )

                application {
                    initializeSerialization(builder = builder)
                }

                routing {
                    entityRoutes()
                }

                val client = createClient {
                    install(ContentNegotiation) {
                        json(
                            Json {
                                serializersModule = SerializersModule {
                                    include(apiModuleSerializersModule)
                                }
                            },
                        )
                    }
                }

                val response = client.post("/api/entities/actions") {
                    contentType(ContentType.Application.Json)
                    setBody<EntityActionRequest?>(
                        body = EntityActionApiMapper.invoke(
                            source = OnOff.Toggle.On(
                                entity = EntityIdentifier("light.test"),
                            ),
                        ),
                    )
                }

                expectThat(response)
                    .get(HttpResponse::status)
                    .isEqualTo(HttpStatusCode.NotFound)
            }
        }

        test(name = "When an entity identifier is not matching the request returns http status not found") {
            testApplication {
                initializeDependencyInjection(
                    executeEntityAction = {
                        ExecuteEntityAction.Result.Rejected(
                            reason = EntityActionDecision.Reason.MismatchedEntity,
                        )
                    },
                )

                application {
                    initializeSerialization(builder = builder)
                }

                routing {
                    entityRoutes()
                }

                val client = createClient {
                    install(ContentNegotiation) {
                        json(
                            Json {
                                serializersModule = SerializersModule {
                                    include(apiModuleSerializersModule)
                                }
                            },
                        )
                    }
                }

                val response = client.post("/api/entities/actions") {
                    contentType(ContentType.Application.Json)
                    setBody<EntityActionRequest?>(
                        body = EntityActionApiMapper.invoke(
                            source = OnOff.Toggle.On(
                                entity = EntityIdentifier("light.test"),
                            ),
                        ),
                    )
                }

                expectThat(response)
                    .get(HttpResponse::status)
                    .isEqualTo(HttpStatusCode.NotFound)
            }
        }

        test(name = "When an entity action is already pending the request returns http status conflict") {
            testApplication {
                initializeDependencyInjection(
                    executeEntityAction = {
                        ExecuteEntityAction.Result.Rejected(
                            reason = EntityActionDecision.Reason.DuplicateAction,
                        )
                    },
                )

                application {
                    initializeSerialization(builder = builder)
                }

                routing {
                    entityRoutes()
                }

                val client = createClient {
                    install(ContentNegotiation) {
                        json(
                            Json {
                                serializersModule = SerializersModule {
                                    include(apiModuleSerializersModule)
                                }
                            },
                        )
                    }
                }

                val response = client.post("/api/entities/actions") {
                    contentType(ContentType.Application.Json)
                    setBody<EntityActionRequest?>(
                        body = EntityActionApiMapper.invoke(
                            source = OnOff.Toggle.On(
                                entity = EntityIdentifier("light.test"),
                            ),
                        ),
                    )
                }

                expectThat(response)
                    .get(HttpResponse::status)
                    .isEqualTo(HttpStatusCode.Conflict)
            }
        }

        test(name = "When an entity action is obsolete (state already reached) the request returns http status not acceptable") {
            testApplication {
                initializeDependencyInjection(
                    executeEntityAction = {
                        ExecuteEntityAction.Result.Rejected(
                            reason = EntityActionDecision.Reason.DuplicateState,
                        )
                    },
                )

                application {
                    initializeSerialization(builder = builder)
                }

                routing {
                    entityRoutes()
                }

                val client = createClient {
                    install(ContentNegotiation) {
                        json(
                            Json {
                                serializersModule = SerializersModule {
                                    include(apiModuleSerializersModule)
                                }
                            },
                        )
                    }
                }

                val response = client.post("/api/entities/actions") {
                    contentType(ContentType.Application.Json)
                    setBody<EntityActionRequest?>(
                        body = EntityActionApiMapper.invoke(
                            source = OnOff.Toggle.On(
                                entity = EntityIdentifier("light.test"),
                            ),
                        ),
                    )
                }

                expectThat(response)
                    .get(HttpResponse::status)
                    .isEqualTo(HttpStatusCode.NotAcceptable)
            }
        }

        test(name = "When an entity action is unsupported the request returns http status unprocessable entity") {
            testApplication {
                initializeDependencyInjection(
                    executeEntityAction = {
                        ExecuteEntityAction.Result.Rejected(
                            reason = EntityActionDecision.Reason.Unsupported,
                        )
                    },
                )

                application {
                    initializeSerialization(builder = builder)
                }

                routing {
                    entityRoutes()
                }

                val client = createClient {
                    install(ContentNegotiation) {
                        json(
                            Json {
                                serializersModule = SerializersModule {
                                    include(apiModuleSerializersModule)
                                }
                            },
                        )
                    }
                }

                val response = client.post("/api/entities/actions") {
                    contentType(ContentType.Application.Json)
                    setBody<EntityActionRequest?>(
                        body = EntityActionApiMapper.invoke(
                            source = OnOff.Toggle.On(
                                entity = EntityIdentifier("light.test"),
                            ),
                        ),
                    )
                }

                expectThat(response)
                    .get(HttpResponse::status)
                    .isEqualTo(HttpStatusCode.UnprocessableEntity)
            }
        }

        test(name = "When an invalid json is sent the request returns http status bad request") {
            testApplication {
                initializeDependencyInjection(
                    executeEntityAction = {
                        ExecuteEntityAction.Result.Accepted
                    },
                )

                application {
                    initializeSerialization(builder = builder)
                }

                routing {
                    entityRoutes()
                }

                val client = createClient {
                    install(ContentNegotiation) {
                        json(
                            Json {
                                serializersModule = SerializersModule {
                                    include(apiModuleSerializersModule)
                                }
                            },
                        )
                    }
                }

                val response = client.post("/api/entities/actions") {
                    contentType(ContentType.Application.Json)
                    setBody(
                        body = "{}",
                    )
                }

                expectThat(response)
                    .get(HttpResponse::status)
                    .isEqualTo(HttpStatusCode.BadRequest)
            }
        }
    }
}

private fun ApplicationTestBuilder.initializeDependencyInjection(
    entities: List<Entity>,
) {
    install(Koin) {
        modules(
            applicationModule,

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

private fun ApplicationTestBuilder.initializeDependencyInjection(
    executeEntityAction: ExecuteEntityAction,
) {
    install(Koin) {
        modules(
            applicationModule,

            module {
                single<ExecuteEntityAction> { executeEntityAction }
            },
        )
    }
}

private fun host() = System.Host(
    device = System.Host.Device(
        manufacturer = "Raspberry Pi",
        model = "4B",
        architecture = "x64",
        physicalCores = 1,
        logicalCores = 4,
    ),
    operatingSystem = System.Host.OperatingSystem(
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
    load = required(MeasureLoad(current = load)),
    temperature = optional(temperature?.let { MeasureTemperature(current = it) }),
)

private fun memory(
    total: Quantity<Dimension.DigitalInformation> = 8.gibibytes,
    available: Quantity<Dimension.DigitalInformation> = 4.gibibytes,
    swapTotal: Quantity<Dimension.DigitalInformation> = 2.gibibytes,
    swapAvailable: Quantity<Dimension.DigitalInformation> = 1.gibibytes,
) = System.Memory(
    total = required(MeasureDataSize(current = total)),
    available = required(MeasureDataSize(current = available)),
    swap = System.Memory.Swap(
        total = required(MeasureDataSize(current = swapTotal)),
        available = required(MeasureDataSize(current = swapAvailable)),
    ),
)

private fun uptime(
    host: Instant = Instant.fromEpochSeconds(0),
    application: Instant = Instant.fromEpochSeconds(3600),
) = System.StartedAt(
    host = host,
    application = application,
)
