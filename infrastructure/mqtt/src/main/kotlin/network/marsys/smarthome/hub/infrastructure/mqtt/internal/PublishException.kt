package network.marsys.smarthome.hub.infrastructure.mqtt.internal

internal class PublishException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)
