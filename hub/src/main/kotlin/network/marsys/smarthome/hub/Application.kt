package network.marsys.smarthome.hub

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.engine.CommandLineConfig
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import network.marsys.smarthome.hub.plugin.initializeAuthentication
import network.marsys.smarthome.hub.plugin.initializeRouting
import network.marsys.smarthome.hub.plugin.initializeSerialization

private val logger = KotlinLogging.logger {}

fun main(args: Array<String>) {
    val config = CommandLineConfig(args)

    embeddedServer(
        factory = Netty,
        environment = config.environment,
        configure = {
            takeFrom(config.engineConfig)
        },
        module = {
            logger.info(::ASCII_LOGO)

            initializeAuthentication()
            initializeSerialization()
            initializeRouting()
        },
    ).start(wait = true)
}

private const val ASCII_LOGO = """
         _____                      _   _   _                        _           _
        /  ___|                    | | | | | |                      | |         | |
        \ `--. _ __ ___   __ _ _ __| |_| |_| | ___  _ __ ___   ___  | |__  _   _| |__
         `--. \ '_ ` _ \ / _` | '__| __|  _  |/ _ \| '_ ` _ \ / _ \ | '_ \| | | | '_ \
        /\__/ / | | | | | (_| | |  | |_| | | | (_) | | | | | |  __/ | | | | |_| | |_) |
        \____/|_| |_| |_|\__,_|_|   \__\_| |_/\___/|_| |_| |_|\___| |_| |_|\__,_|_.__/
                                                                      v2026.01-SNAPSHOT
"""
