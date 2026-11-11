package network.marsys.smarthome.hub.plugin

import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonBuilder
import org.koin.core.parameter.parametersOf
import org.koin.ktor.ext.get

fun Application.initializeSerialization(
    builder: JsonBuilder.() -> Unit = {},
) {
    val json = get<Json> { parametersOf(builder) }

    install(ContentNegotiation) {
        json(json = json)
    }
}
