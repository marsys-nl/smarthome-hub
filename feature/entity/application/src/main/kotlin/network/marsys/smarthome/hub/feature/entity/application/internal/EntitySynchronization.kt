package network.marsys.smarthome.hub.feature.entity.application.internal

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import network.marsys.smarthome.domain.entity.EntityIdentifier
import java.util.concurrent.ConcurrentHashMap

internal class EntitySynchronization {
    private val mutexes =
        ConcurrentHashMap<EntityIdentifier, Mutex>()

    suspend fun <T> withLock(
        entity: EntityIdentifier,
        block: suspend () -> T,
    ): T {
        val mutex = mutexes.computeIfAbsent(entity) {
            Mutex()
        }

        return mutex.withLock {
            block()
        }
    }
}
