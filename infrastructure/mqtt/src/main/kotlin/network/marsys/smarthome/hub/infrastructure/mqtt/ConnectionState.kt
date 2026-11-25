package network.marsys.smarthome.hub.infrastructure.mqtt

sealed interface ConnectionState {
    data object Disconnected : ConnectionState
    data object Connected : ConnectionState
}
