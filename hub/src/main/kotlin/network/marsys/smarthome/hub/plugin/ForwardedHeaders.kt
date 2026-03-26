package network.marsys.smarthome.hub.plugin

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.forwardedheaders.XForwardedHeaders

fun Application.initializeForwardedHeaders() {
    install(XForwardedHeaders) {
        useFirstProxy()
    }
}
