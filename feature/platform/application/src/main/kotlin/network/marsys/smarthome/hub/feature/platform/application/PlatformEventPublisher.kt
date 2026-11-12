package network.marsys.smarthome.hub.feature.platform.application

import network.marsys.smarthome.domain.platform.PlatformEvent

fun interface PlatformEventPublisher {
    suspend fun publish(event: PlatformEvent)
}
