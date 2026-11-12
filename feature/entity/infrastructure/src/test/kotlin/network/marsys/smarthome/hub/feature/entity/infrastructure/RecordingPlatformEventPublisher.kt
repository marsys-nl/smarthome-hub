package network.marsys.smarthome.hub.feature.entity.infrastructure

import network.marsys.smarthome.domain.platform.PlatformEvent
import network.marsys.smarthome.hub.feature.platform.application.PlatformEventPublisher

internal class RecordingPlatformEventPublisher : PlatformEventPublisher {

    val events = mutableListOf<PlatformEvent>()

    override suspend fun publish(event: PlatformEvent) {
        events += event
    }
}
