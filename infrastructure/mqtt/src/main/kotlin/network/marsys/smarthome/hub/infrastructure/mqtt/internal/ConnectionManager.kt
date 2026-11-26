package network.marsys.smarthome.hub.infrastructure.mqtt.internal

import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import network.marsys.smarthome.hub.infrastructure.mqtt.ConnectionState
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

internal class ConnectionManager(
    private val connection: Connection,
    private val demander: ConnectionDemander,
    private val idleTimeout: Duration = 5.seconds,
) {
    val state: StateFlow<ConnectionState>
        field = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)

    init {
        require(idleTimeout.isFinite()) {
            "Idle timeout must be finite"
        }

        require(idleTimeout >= Duration.ZERO) {
            "Idle timeout must be greater than or equal to 0, but was $idleTimeout"
        }
    }

    @OptIn(FlowPreview::class)
    suspend fun run(
        block: suspend () -> Unit,
    ) {
        demander.required
            .debounce { required ->
                if (required) {
                    Duration.ZERO
                } else {
                    idleTimeout
                }
            }
            .distinctUntilChanged()
            .collectLatest { required ->
                if (required) {
                    maintainActiveConnection(block = block)
                }
            }
    }

    fun onClosed() {
        state.value = ConnectionState.Disconnected
    }

    private suspend fun maintainActiveConnection(
        block: suspend () -> Unit,
    ) {
        var connectionAttempt = 0
        var sessionAttempt = 0

        try {
            while (true) {
                currentCoroutineContext()
                    .ensureActive()

                demander.required
                    .first { it }

                try {
                    connect()
                } catch (_: ConnectionException) {
                    updateConnectionState()

                    delay(duration = ReconnectDelay.calculate(connectionAttempt))

                    connectionAttempt = (connectionAttempt + 1).coerceAtMost(6)

                    continue
                }

                connectionAttempt = 0

                try {
                    maintainActiveSession(block = block)
                } catch (_: SessionException) {
                    disconnect()

                    delay(duration = ReconnectDelay.calculate(sessionAttempt))

                    sessionAttempt = (sessionAttempt + 1).coerceAtMost(6)

                    continue
                }

                sessionAttempt = 0

                updateConnectionState()
            }
        } finally {
            disconnect()
        }
    }

    private suspend fun maintainActiveSession(
        block: suspend () -> Unit,
    ) {
        coroutineScope {
            if (!connection.connected.value) {
                return@coroutineScope
            }

            val job = launch {
                block.invoke()
            }

            try {
                connection.connected
                    .first { !it }
            } finally {
                job.cancel()
            }
        }
    }

    private suspend fun connect() {
        withContext(NonCancellable) {
            connection.connect()
        }

        currentCoroutineContext()
            .ensureActive()

        updateConnectionState()
    }

    private suspend fun disconnect() {
        withContext(NonCancellable) {
            try {
                if (connection.connected.value) {
                    connection.disconnect()
                }
            } finally {
                updateConnectionState()
            }
        }
    }

    private fun updateConnectionState() {
        state.value = if (connection.connected.value) {
            ConnectionState.Connected
        } else {
            ConnectionState.Disconnected
        }
    }
}
