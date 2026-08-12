package network.marsys.smarthome.hub.plugin

import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import network.marsys.smarthome.api.apiModuleSerializersModule

fun Application.initializeSerialization() {
    install(ContentNegotiation) {
        json(
            json = Json {
                serializersModule = SerializersModule {
                    include(apiModuleSerializersModule)
                }
            },
        )
    }
}
