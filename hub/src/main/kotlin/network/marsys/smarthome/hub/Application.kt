package network.marsys.smarthome.hub

import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty

fun main() {
    embeddedServer(
        factory = Netty,
        port = 8080,
        module = {
            println("Smarthome hub is running...")
        },
    ).start(wait = true)
}
