package network.marsys.smarthome.hub.feature.platform.application

import kotlinx.coroutines.flow.Flow
import network.marsys.smarthome.domain.platform.PlatformEvent

interface PlatformEventSubscription : AutoCloseable {
    val events: Flow<PlatformEvent>
}
