package network.marsys.smarthome.hub.infrastructure.mqtt.internal

internal interface PublishExecutor {
    suspend fun publish(
        topic: TopicName,
        payload: ByteArray,
    )
}
