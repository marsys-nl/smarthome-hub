package network.marsys.smarthome.hub.routes

import io.ktor.server.routing.Route
import io.ktor.server.sse.heartbeat
import io.ktor.server.sse.sse
import io.ktor.sse.ServerSentEvent
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonBuilder
import network.marsys.smarthome.api.mappers.PlatformEventApiMapper
import network.marsys.smarthome.hub.feature.platform.application.PlatformEventStream
import org.koin.core.parameter.parametersOf
import org.koin.ktor.ext.get
import kotlin.time.Duration.Companion.seconds

fun Route.eventRoutes() {
    val platformEventStream = get<PlatformEventStream>()

    val json = get<Json> {
        val builder: JsonBuilder.() -> Unit = {
            encodeDefaults = false
            ignoreUnknownKeys = true
        }

        parametersOf(builder)
    }

    sse(path = "/api/events") {
        val subscription = platformEventStream.subscribe()

        try {
            heartbeat {
                period = 30.seconds
                event = ServerSentEvent(
                    event = "heartbeat",
                )
            }

            send(
                event = ServerSentEvent(
                    event = "sync-required",
                    data = """{"scope":"all"}""",
                ),
            )

            subscription.events.collect { event ->
                send(
                    event = ServerSentEvent(
                        event = "platform-event",
                        data = json.encodeToString(
                            value = PlatformEventApiMapper.invoke(source = event),
                        ),
                    ),
                )
            }
        } finally {
            subscription.close()
        }
    }
}
