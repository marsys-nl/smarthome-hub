package network.marsys.smarthome.hub.feature.integration.application

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import network.marsys.smarthome.hub.feature.integration.application.ports.outbound.IntegrationRuntime
import network.marsys.smarthome.hub.feature.integration.domain.Integration
import kotlin.coroutines.cancellation.CancellationException

class IntegrationLifecycleController(
    private val scope: CoroutineScope,
    initialStatus: Integration.Status = Integration.Status.Stopped,
    private val onStart: suspend CoroutineScope.() -> Unit = {},
    private val onStop: suspend () -> Unit = {},
) : IntegrationRuntime {
    private val logger = KotlinLogging.logger { }

    val status: StateFlow<Integration.Status>
        field = MutableStateFlow(value = initialStatus)

    private val mutex = Mutex()
    private var job: Job? = null

    override suspend fun start() = mutex
        .withLock {
            startInternal()
        }
        .await()

    override suspend fun stop() = mutex
        .withLock {
            stopInternal()
        }

    override suspend fun restart() = mutex
        .withLock {
            stopInternal()
            startInternal()
        }
        .await()

    private suspend fun startInternal(): Deferred<Unit> {
        if (
            status.value is Integration.Status.Failed ||
            status.value == Integration.Status.Degraded
        ) {
            stopInternal()
        }

        if (status.value != Integration.Status.Stopped) {
            return CompletableDeferred(Unit)
        }

        currentCoroutineContext().ensureActive()
        scope.ensureActive()

        val deferred = CompletableDeferred<Unit>()

        status.value = Integration.Status.Starting

        job = scope.launch {
            try {
                coroutineScope {
                    onStart.invoke(this)

                    if (
                        status.compareAndSet(
                            expect = Integration.Status.Starting,
                            update = Integration.Status.Running,
                        )
                    ) {
                        deferred.complete(Unit)
                    }

                    awaitCancellation()
                }
            } catch (throwable: Throwable) {
                status.update { current ->
                    if (current == Integration.Status.Stopping) {
                        current
                    } else {
                        Integration.Status.Failed(throwable)
                    }
                }

                deferred.completeExceptionally(exception = throwable)

                if (throwable is CancellationException) {
                    throw throwable
                }
            }
        }.also { job ->
            job.invokeOnCompletion { cause ->
                deferred.completeExceptionally(exception = cause ?: return@invokeOnCompletion)
            }
        }

        return deferred
    }

    private suspend fun stopInternal() {
        when (status.value) {
            Integration.Status.Stopped, Integration.Status.Stopping -> return
            else -> Unit
        }

        status.value = Integration.Status.Stopping

        withContext(NonCancellable) {
            try {
                job?.cancelAndJoin()
                job = null

                onStop.invoke()

                status.value = Integration.Status.Stopped
            } catch (throwable: Throwable) {
                status.value = Integration.Status.Failed(throwable)
                throw throwable
            }
        }
    }
}
