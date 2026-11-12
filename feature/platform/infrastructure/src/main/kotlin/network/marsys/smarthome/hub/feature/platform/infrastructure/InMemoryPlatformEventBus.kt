package network.marsys.smarthome.hub.feature.platform.infrastructure

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.onCompletion
import network.marsys.smarthome.domain.platform.PlatformEvent
import network.marsys.smarthome.hub.feature.platform.application.PlatformEventPublisher
import network.marsys.smarthome.hub.feature.platform.application.PlatformEventStream
import network.marsys.smarthome.hub.feature.platform.application.PlatformEventSubscription
import kotlin.coroutines.cancellation.CancellationException

class InMemoryPlatformEventBus(
    private val capacity: Int = 256,
) : PlatformEventPublisher, PlatformEventStream {
    private val lock = Any()

    private val subscribers = mutableSetOf<Channel<PlatformEvent>>()

    init {
        require(capacity > 0) { "Buffer capacity must be greater than 0" }
    }

    override suspend fun publish(event: PlatformEvent) {
        synchronized(lock) {
            val iterator = subscribers.iterator()

            while (iterator.hasNext()) {
                val channel = iterator.next()

                if (channel.trySend(event).isFailure) {
                    iterator.remove()

                    channel.cancel(
                        cause = CancellationException("Platform event subscription overflow"),
                    )
                }
            }
        }
    }

    override suspend fun subscribe(): PlatformEventSubscription {
        val channel = Channel<PlatformEvent>(capacity = capacity)

        synchronized(lock) {
            subscribers.add(channel)
        }

        return object : PlatformEventSubscription {
            override val events: Flow<PlatformEvent> =
                channel
                    .consumeAsFlow()
                    .onCompletion { close() }

            override fun close() {
                synchronized(lock) {
                    subscribers.remove(channel)
                    channel.cancel()
                }
            }
        }
    }
}
