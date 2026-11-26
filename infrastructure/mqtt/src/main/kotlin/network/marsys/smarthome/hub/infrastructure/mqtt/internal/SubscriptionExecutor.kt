package network.marsys.smarthome.hub.infrastructure.mqtt.internal

internal interface SubscriptionExecutor {
    suspend fun subscribe(filter: TopicFilter)
    suspend fun unsubscribe(filter: TopicFilter)
}
