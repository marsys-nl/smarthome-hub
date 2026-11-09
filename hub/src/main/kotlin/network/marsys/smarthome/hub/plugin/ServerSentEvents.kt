package network.marsys.smarthome.hub.plugin

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.sse.SSE

fun Application.initializeServerSentEvents() {
    install(SSE)
}
