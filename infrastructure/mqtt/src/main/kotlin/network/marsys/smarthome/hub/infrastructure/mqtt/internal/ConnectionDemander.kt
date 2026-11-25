package network.marsys.smarthome.hub.infrastructure.mqtt.internal

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

internal class ConnectionDemander {
    private val consumers = MutableStateFlow(0)

    val required: Flow<Boolean> = consumers
        .map { it > 0 }
        .distinctUntilChanged()

    suspend fun <T> demand(block: suspend () -> T): T {
        consumers.update { it + 1 }

        return try {
            block.invoke()
        } finally {
            consumers.update { it - 1 }
        }
    }
}
