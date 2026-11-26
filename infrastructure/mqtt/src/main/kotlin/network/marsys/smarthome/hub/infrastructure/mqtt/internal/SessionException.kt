package network.marsys.smarthome.hub.infrastructure.mqtt.internal

internal class SessionException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)
