package network.marsys.smarthome.hub.infrastructure.mqtt.internal

import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import network.marsys.smarthome.hub.infrastructure.mqtt.ConnectionState
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

internal class Publisher(
    private val executor: PublishExecutor,
    private val demander: ConnectionDemander,
    private val state: StateFlow<ConnectionState>,
    private val connectionTimeout: Duration = 10.seconds,
    private val publishTimeout: Duration = 10.seconds,
) {
    init {
        require(connectionTimeout.isFinite()) {
            "Connection timeout must be finite"
        }

        require(connectionTimeout >= Duration.ZERO) {
            "Connection timeout must be greater than or equal to 0, but was $connectionTimeout"
        }

        require(publishTimeout.isFinite()) {
            "Publish timeout must be finite"
        }

        require(publishTimeout >= Duration.ZERO) {
            "Publish timeout must be greater than or equal to 0, but was $publishTimeout"
        }
    }

    suspend fun publish(topic: TopicName, payload: ByteArray) {
        val message = payload.copyOf()

        demander.demand {
            awaitConnection(topic = topic)

            withTimeoutOrNull(timeout = publishTimeout) {
                executor.publish(
                    topic = topic,
                    payload = message,
                )
            } ?: throw PublishException(
                message = "Timed out waiting for MQTT publish to topic '${topic.value}' to complete.",
            )
        }
    }

    private suspend fun awaitConnection(topic: TopicName) {
        withTimeoutOrNull(timeout = connectionTimeout) {
            state.first { it == ConnectionState.Connected }
        } ?: throw PublishException(
            message = "Timed out waiting for MQTT connection to be " +
                "established before publishing to topic '${topic.value}'.",
        )
    }
}
