package network.marsys.smarthome.hub.feature.platform.application

fun interface PlatformEventStream {
    suspend fun subscribe(): PlatformEventSubscription
}
