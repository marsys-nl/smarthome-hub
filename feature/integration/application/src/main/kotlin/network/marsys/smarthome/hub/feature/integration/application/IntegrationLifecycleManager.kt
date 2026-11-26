package network.marsys.smarthome.hub.feature.integration.application

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withTimeout
import network.marsys.smarthome.domain.identifiers.IntegrationIdentifier
import network.marsys.smarthome.hub.feature.integration.application.exception.IntegrationNotFoundException
import network.marsys.smarthome.hub.feature.integration.application.ports.inbound.IntegrationQueries
import network.marsys.smarthome.hub.feature.integration.application.ports.inbound.ManageIntegrationLifecycle
import network.marsys.smarthome.hub.feature.integration.domain.Integration
import kotlin.time.Duration.Companion.seconds

class IntegrationLifecycleManager(
    private val integrations: List<IntegrationAdapter>,
) : IntegrationQueries, ManageIntegrationLifecycle {
    private val logger = KotlinLogging.logger {}

    override fun all(): Collection<IntegrationAdapter> = integrations

    override suspend fun start(identifier: IntegrationIdentifier) =
        start(
            integration = integrations.find(identifier = identifier),
        )

    override suspend fun stop(identifier: IntegrationIdentifier) =
        stop(
            integration = integrations.find(identifier = identifier),
        )

    override suspend fun restart(identifier: IntegrationIdentifier) =
        restart(
            integration = integrations.find(identifier = identifier),
        )

    suspend fun start() {
        logger.info { "Starting ${integrations.size} integrations." }

        integrations.forEach { integration ->
            try {
                start(integration = integration)
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                logger.error(exception) {
                    "Failed to start integration '${integration.identifier}'. See exception for more information."
                }
            }
        }
    }

    suspend fun stop() = supervisorScope {
        integrations.forEach { integration ->
            try {
                stop(integration = integration)
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                logger.error(exception) {
                    "Failed to stop integration '${integration.identifier}'. See exception for more information."
                }
            }
        }
    }

    private suspend fun start(integration: IntegrationAdapter) {
        logger.info { "Starting integration '${integration.identifier}'." }
        integration.start()
    }

    private suspend fun stop(integration: IntegrationAdapter) {
        logger.info { "Stopping integration '${integration.identifier}'." }
        integration.stop()
    }

    private suspend fun restart(integration: IntegrationAdapter) {
        logger.info { "Restarting integration '${integration.identifier}'." }
        integration.restart()
    }

    private fun List<IntegrationAdapter>.find(identifier: IntegrationIdentifier): IntegrationAdapter =
        find { it.identifier == identifier }
            ?: throw IntegrationNotFoundException(identifier)
}
