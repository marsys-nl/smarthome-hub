package network.marsys.smarthome.hub.feature.integration.application

import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import network.marsys.smarthome.hub.feature.integration.application.ports.outbound.IntegrationRuntime
import network.marsys.smarthome.hub.feature.integration.domain.Integration
import kotlin.coroutines.cancellation.CancellationException

class IntegrationLifecycleController(
    initialStatus: Integration.Status = Integration.Status.Stopped,
    private val onStart: suspend () -> Unit = {},
    private val onPrepareStop: suspend () -> Unit = {},
    private val onStop: suspend () -> Unit = {},
) : IntegrationRuntime {
    private val statusStateFlow = MutableStateFlow(value = initialStatus)
    val status: StateFlow<Integration.Status> = statusStateFlow.asStateFlow()

    private var lifecycleJob: Job? = null

    override suspend fun start() = coroutineScope {
        if (status.isRunning) {
            return@coroutineScope
        }

        statusStateFlow.update { Integration.Status.Starting }

        lifecycleJob = launch {
            try {
                onStart.invoke()
                statusStateFlow.update { Integration.Status.Running }
            } catch (_: CancellationException) {
                // Whenever the lifecycle job is canceled, we don't want to set
                // the status to failed, because it was a controlled cancellation.
            } catch (throwable: Throwable) {
                statusStateFlow.update { Integration.Status.Failed(throwable) }
                throw throwable
            }
        }
    }

    override suspend fun stop() {
        if (status.isStopping) {
            return
        }

        statusStateFlow.update { Integration.Status.Stopping }

        try {
            onPrepareStop.invoke()

            lifecycleJob?.cancelAndJoin()
            lifecycleJob = null

            onStop.invoke()

            statusStateFlow.update { Integration.Status.Stopped }
        } catch (throwable: Throwable) {
            statusStateFlow.update { Integration.Status.Failed(throwable) }
            throw throwable
        }
    }

    private val StateFlow<Integration.Status>.isRunning: Boolean
        get() = value !is Integration.Status.Stopped &&
            value !is Integration.Status.Failed

    private val StateFlow<Integration.Status>.isStopping: Boolean
        get() = value is Integration.Status.Stopping ||
            value is Integration.Status.Stopped ||
            value is Integration.Status.Failed
}
