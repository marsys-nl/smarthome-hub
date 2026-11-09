package network.marsys.smarthome.hub.routes

import io.ktor.server.routing.Route
import io.ktor.server.sse.heartbeat
import io.ktor.server.sse.sse
import io.ktor.sse.ServerSentEvent
import kotlinx.coroutines.awaitCancellation
import kotlin.time.Duration.Companion.seconds

fun Route.eventRoutes() {
    sse(path = "/api/events") {
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

        awaitCancellation()
    }
}
