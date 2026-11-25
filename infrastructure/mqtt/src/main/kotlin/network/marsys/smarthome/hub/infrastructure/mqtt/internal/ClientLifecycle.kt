package network.marsys.smarthome.hub.infrastructure.mqtt.internal

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.supervisorScope

internal class ClientLifecycle {
    private val closed = CompletableDeferred<ClientClosedException>()

    fun close(cause: ClientClosedException = ClientClosedException()) {
        closed.complete(cause)
    }

    suspend fun ensureOpen() {
        if (closed.isCompleted) {
            throw closed.await()
        }
    }

    suspend fun <T> execute(
        block: suspend () -> T,
    ): T = supervisorScope {
        ensureOpen()

        val operation = async(start = CoroutineStart.LAZY) {
            ensureOpen()
            block.invoke()
        }

        try {
            select {
                closed.onAwait { cause ->
                    throw cause
                }

                operation.onAwait { result ->
                    result
                }
            }
        } finally {
            operation.cancel()
        }
    }
}
