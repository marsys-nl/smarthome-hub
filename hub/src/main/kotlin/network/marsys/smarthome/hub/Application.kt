package network.marsys.smarthome.hub

import io.ktor.server.application.port
import io.ktor.server.engine.CommandLineConfig
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty

fun main(args: Array<String>) {
    val config = CommandLineConfig(args)

    embeddedServer(
        factory = Netty,
        environment = config.environment,
        configure = {
            takeFrom(config.engineConfig)
        },
        module = {
            println("Application started at port ${environment.config.port}. Development mode: $developmentMode")
        },
    ).start(wait = true)
}
