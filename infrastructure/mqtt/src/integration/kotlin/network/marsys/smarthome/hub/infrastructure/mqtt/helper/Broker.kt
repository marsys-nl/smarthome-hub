package network.marsys.smarthome.hub.infrastructure.mqtt.helper

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import network.marsys.smarthome.hub.infrastructure.mqtt.container.MosquittoContainer

internal suspend fun <T> withBroker(
    block: suspend (MosquittoContainer) -> T,
): T = withContext(Dispatchers.IO) {
    MosquittoContainer().use { broker ->
        broker.start()
        block.invoke(broker)
    }
}
