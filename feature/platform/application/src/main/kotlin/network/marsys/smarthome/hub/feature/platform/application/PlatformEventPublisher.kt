package network.marsys.smarthome.hub.feature.platform.application

fun interface PlatformEventPublisher {
    suspend fun publish(event: PlatformEvent)
}
