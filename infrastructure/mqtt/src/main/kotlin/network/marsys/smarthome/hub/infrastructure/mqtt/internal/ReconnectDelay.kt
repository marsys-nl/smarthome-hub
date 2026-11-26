package network.marsys.smarthome.hub.infrastructure.mqtt.internal

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

internal object ReconnectDelay {
    fun calculate(attempt: Int): Duration {
        require(attempt >= 0) {
            "Attempt must be greater than 0, but was $attempt"
        }

        return (1L shl attempt.coerceAtMost(MAX_RECONNECT_ATTEMPTS))
            .coerceAtMost(MAX_RECONNECT_DELAY)
            .seconds
    }

    internal const val MAX_RECONNECT_ATTEMPTS = 6
    internal const val MAX_RECONNECT_DELAY = 60L
}
