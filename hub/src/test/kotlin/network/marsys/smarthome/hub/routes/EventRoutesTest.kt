package network.marsys.smarthome.hub.routes

import de.infix.testBalloon.framework.core.testSuite
import dev.nmarsman.expect.api.expectThat
import dev.nmarsman.expect.assertions.first
import dev.nmarsman.expect.assertions.hasSize
import dev.nmarsman.expect.assertions.isA
import dev.nmarsman.expect.assertions.isEqualTo
import dev.nmarsman.expect.assertions.isTrue
import dev.nmarsman.expect.assertions.last
import io.ktor.client.plugins.sse.SSE
import io.ktor.client.plugins.sse.sse
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.ktor.sse.ServerSentEvent
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonBuilder
import kotlinx.serialization.modules.SerializersModule
import network.marsys.smarthome.api.apiModuleSerializersModule
import network.marsys.smarthome.api.models.platform.CapabilityMessage
import network.marsys.smarthome.api.models.platform.ContextMessage
import network.marsys.smarthome.api.models.platform.PlatformEventMessage
import network.marsys.smarthome.domain.capability.OnOff
import network.marsys.smarthome.domain.entity.EntityIdentifier
import network.marsys.smarthome.domain.platform.PlatformEvent
import network.marsys.smarthome.hub.feature.platform.application.PlatformEventStream
import network.marsys.smarthome.hub.feature.platform.infrastructure.InMemoryPlatformEventBus
import network.marsys.smarthome.hub.plugin.initializeServerSentEvents
import org.koin.dsl.binds
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds

val EventRoutesTest by testSuite(
    name = "Event routes tests",
) {
    val json = Json {
        serializersModule = SerializersModule {
            include(apiModuleSerializersModule)
        }
    }

    test(name = "Published capability update is delivered to subscribers") {
        val bus = InMemoryPlatformEventBus()

        testApplication {
            initializeDependencyInjection(
                bus = bus,
                json = json,
            )

            application {
                initializeServerSentEvents()
            }

            routing {
                eventRoutes()
            }

            val event = PlatformEvent.CapabilityUpdated(
                identifier = EntityIdentifier("light.living-room"),
                capability = OnOff(current = true),
                occurredAt = Clock.System.now(),
            )

            val client = createClient {
                install(SSE)
            }

            client.sse("/api/events") {
                val received = withTimeout(5.seconds) {
                    incoming
                        .filter { it.event != "heartbeat" }
                        .onEach { message ->
                            if (message.event == "sync-required") {
                                bus.publish(event)
                            }
                        }
                        .take(2)
                        .toList()
                }

                expectThat(received)
                    .first()
                    .get(ServerSentEvent::event)
                    .isEqualTo("sync-required")

                expectThat(received)
                    .hasSize(2)
                    .last()
                    .with(ServerSentEvent::event) { isEqualTo("platform-event") }

                val message = json.decodeFromString<PlatformEventMessage>(received.last().data!!)

                expectThat(message)
                    .isA<PlatformEventMessage.CapabilityUpdated>()
                    .with(PlatformEventMessage.CapabilityUpdated::identifier) { isEqualTo(event.identifier.value) }
                    .get(PlatformEventMessage.CapabilityUpdated::capability)
                    .isA<CapabilityMessage.BooleanValue>()
                    .with(CapabilityMessage.BooleanValue::type) { isEqualTo("onOff") }
                    .with(CapabilityMessage.BooleanValue::context) { isEqualTo(ContextMessage(emptyList())) }
                    .with(CapabilityMessage.BooleanValue::current) { isTrue() }
            }
        }
    }
}

private fun ApplicationTestBuilder.initializeDependencyInjection(
    bus: InMemoryPlatformEventBus,
    json: Json,
) {
    install(Koin) {
        modules(
            module {
                factory<Json> { (builder: JsonBuilder.() -> Unit) ->
                    Json(from = json) {
                        builder.invoke(this)
                    }
                }

                single { bus } binds arrayOf(
                    PlatformEventStream::class,
                )
            },
        )
    }
}
