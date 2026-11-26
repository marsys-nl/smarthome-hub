package network.marsys.smarthome.hub.infrastructure.mqtt.internal

import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import network.marsys.smarthome.hub.infrastructure.mqtt.Message

internal class SubscriptionRegistry {
    private val lock = Any()

    private var closed: ClientClosedException? = null
    private val subscriptions = mutableMapOf<SendChannel<Message>, Set<TopicFilter>>()

    val required: StateFlow<Set<TopicFilter>>
        field = MutableStateFlow<Set<TopicFilter>>(emptySet())

    fun close(
        cause: ClientClosedException = ClientClosedException(),
    ) {
        val recipients = synchronized(lock) {
            if (closed != null) {
                return
            }

            closed = cause

            subscriptions.keys.toList().also {
                subscriptions.clear()
                updateRequiredFilters()
            }
        }

        recipients.forEach { channel ->
            channel.close(cause)
        }
    }

    fun subscribe(filters: Set<TopicFilter>): Flow<Message> {
        require(filters.isNotEmpty()) {
            "At least one filter must be provided."
        }

        val unique = filters.toSet()

        return callbackFlow {
            register(
                channel = channel,
                filters = unique,
            )

            try {
                awaitClose()
            } finally {
                unregister(channel = channel)
            }
        }
    }

    fun dispatch(message: Message) {
        val recipients = synchronized(lock) {
            subscriptions
                .filterValues { filters ->
                    filters.any { it.matches(message.topic) }
                }
                .keys
                .toList()
        }

        recipients.forEach { channel ->
            val result = channel.trySend(message)

            if (result.isFailure && !result.isClosed) {
                channel.close(
                    cause = IllegalStateException("MQTT subscription buffer is full."),
                )
            }
        }
    }

    fun reject(filter: TopicFilter, cause: SubscriptionException) {
        val recipients = synchronized(lock) {
            val matching = subscriptions
                .filterValues { filters ->
                    filter in filters
                }
                .keys
                .toList()

            matching.also {
                it.forEach(subscriptions::remove)
                updateRequiredFilters()
            }
        }

        recipients.forEach { channel ->
            channel.close(cause)
        }
    }

    private fun register(channel: SendChannel<Message>, filters: Set<TopicFilter>) {
        synchronized(lock) {
            closed?.let { throw it }

            subscriptions[channel] = filters
            updateRequiredFilters()
        }
    }

    private fun unregister(channel: SendChannel<Message>) {
        synchronized(lock) {
            subscriptions.remove(channel)
            updateRequiredFilters()
        }
    }

    private fun updateRequiredFilters() {
        required.value = subscriptions.values
            .flatten()
            .toSet()
    }
}
