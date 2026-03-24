package network.marsys.smarthome.hub.plugin

import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation

suspend fun Application.initializeSerialization() {
    install(ContentNegotiation) {
        json()
    }
}
