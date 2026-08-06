package network.marsys.smarthome.hub.feature.integration.application

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private val logger = KotlinLogging.logger {}

class IntegrationLifecycleManager(
    private val integrations: List<IntegrationAdapter>,
) {
    fun start() {
        logger.info { "Starting ${integrations.size} integrations." }
        integrations.forEach { integration ->
            logger.info { "Starting integration: ${integration.identifier}." }
            integration.start()
        }
    }

    suspend fun stop(
        timeout: Duration = 5.seconds,
    ) = supervisorScope {
        integrations
            .map { integration ->
                async {
                    runCatching {
                        withTimeout(timeout) {
                            logger.info { "Stopping integration: ${integration.identifier}." }
                            integration.stop()
                        }
                    }.onFailure {
                        handleIntegrationStopFailure(
                            integration = integration,
                            exception = it,
                        )
                    }
                }
            }
            .awaitAll()

        logger.info { "Finished stopping integrations." }
    }

    private fun handleIntegrationStopFailure(integration: IntegrationAdapter, exception: Throwable) =
        when (exception) {
            is CancellationException -> throw exception
            else -> logger.error(exception) { "Failed to stop integration: ${integration.identifier}." }
        }
}
