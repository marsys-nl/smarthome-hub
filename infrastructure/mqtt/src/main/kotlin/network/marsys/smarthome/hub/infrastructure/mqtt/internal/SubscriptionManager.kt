package network.marsys.smarthome.hub.infrastructure.mqtt.internal

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first

internal class SubscriptionManager(
    private val executor: SubscriptionExecutor,
    private val registry: SubscriptionRegistry,
) {
    suspend fun run() {
        val active = mutableSetOf<TopicFilter>()

        while (true) {
            currentCoroutineContext()
                .ensureActive()

            val required = registry.required
                .first { it != active }

            val added = required - active

            if (added.isNotEmpty()) {
                val filter = added.first()

                try {
                    executor.subscribe(filter)
                    active.add(filter)
                } catch (exception: SubscriptionException) {
                    registry.reject(
                        filter = filter,
                        cause = exception,
                    )
                }
            } else {
                val filter = (active - required).first()
                executor.unsubscribe(filter)
                active.remove(filter)
            }
        }
    }
}
