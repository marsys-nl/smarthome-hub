package network.marsys.smarthome.hub.infrastructure.mqtt.internal

internal class ClientClosedException(
    cause: Throwable? = null,
) : IllegalStateException("MQTT client is closed.", cause)
