package network.marsys.smarthome.hub.feature.integration.application

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withTimeout
import network.marsys.smarthome.domain.identifiers.IntegrationIdentifier
import network.marsys.smarthome.hub.feature.integration.application.exception.IntegrationNotFoundException
import network.marsys.smarthome.hub.feature.integration.application.ports.inbound.ManageIntegrationLifecycle
import network.marsys.smarthome.hub.feature.integration.domain.Integration
import kotlin.time.Duration.Companion.seconds

private val logger = KotlinLogging.logger {}

class IntegrationLifecycleManager(
    private val integrations: List<IntegrationAdapter>,
) : ManageIntegrationLifecycle {
    override suspend fun restart(identifier: IntegrationIdentifier) {
        val integration = integrations.find { it.identifier == identifier }
            ?: throw IntegrationNotFoundException(identifier)

        if (integration.status.value == Integration.Status.Running) {
            stop(integration = integration)
        }

        start(integration = integration)
    }

    fun start() {
        val startableIntegrations = integrations
            .filter { it.status.value == Integration.Status.Stopped }

        logger.info { "Starting ${startableIntegrations.size} integrations." }
        startableIntegrations.forEach(::start)
    }

    override fun start(identifier: IntegrationIdentifier) {
        val integration = integrations.find { it.identifier == identifier }
            ?: throw IntegrationNotFoundException(identifier)

        start(integration = integration)
    }

    suspend fun stop() = supervisorScope {
        integrations
            .filter { it.status.value == Integration.Status.Running }
            .map { integration ->
                async {
                    stop(integration = integration)
                }
            }
            .awaitAll()

        logger.info { "Finished stopping integrations." }
    }

    override suspend fun stop(identifier: IntegrationIdentifier) {
        val integration = integrations.find { it.identifier == identifier }
            ?: throw IntegrationNotFoundException(identifier)

        if (integration.status.value == Integration.Status.Running) {
            stop(integration = integration)
        }
    }

    private fun start(integration: IntegrationAdapter) = try {
        check(integration.status.value == Integration.Status.Stopped) {
            "Can't start integration '${integration.identifier}', as it is already running."
        }

        logger.info { "Starting integration '${integration.identifier}'." }

        integration.start()
    } catch (exception: RuntimeException) {
        logger.info(exception) {
            "Failed to start integration '${integration.identifier}'. See exception for more information."
        }
    }

    private suspend fun stop(integration: IntegrationAdapter) = try {
        check(integration.status.value == Integration.Status.Running) {
            "Can't stop integration '${integration.identifier}', as it is already stopped."
        }

        logger.info { "Stopping integration '${integration.identifier}'." }

        withTimeout(30.seconds) {
            integration.stop()
        }
    } catch (exception: RuntimeException) {
        logger.info(exception) {
            "Failed to stop integration '${integration.identifier}'. See exception for more information."
        }
    }
}
