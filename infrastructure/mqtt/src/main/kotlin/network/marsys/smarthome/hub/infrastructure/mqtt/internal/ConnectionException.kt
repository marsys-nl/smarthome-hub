package network.marsys.smarthome.hub.infrastructure.mqtt.internal

internal class ConnectionException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)
