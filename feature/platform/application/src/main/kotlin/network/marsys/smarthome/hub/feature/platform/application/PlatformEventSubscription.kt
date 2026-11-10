package network.marsys.smarthome.hub.feature.platform.application

import kotlinx.coroutines.flow.Flow

interface PlatformEventSubscription : AutoCloseable {
    val events: Flow<PlatformEvent>
}
