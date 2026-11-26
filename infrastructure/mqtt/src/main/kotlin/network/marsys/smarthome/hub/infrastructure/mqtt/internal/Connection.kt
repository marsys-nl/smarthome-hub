package network.marsys.smarthome.hub.infrastructure.mqtt.internal

import kotlinx.coroutines.flow.StateFlow

interface Connection {
    val connected: StateFlow<Boolean>

    suspend fun connect()
    suspend fun disconnect()
}
