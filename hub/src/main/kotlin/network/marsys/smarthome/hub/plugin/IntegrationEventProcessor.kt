package network.marsys.smarthome.hub.plugin

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStarted
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.application.ApplicationStopping
import kotlinx.coroutines.launch
import network.marsys.smarthome.hub.feature.integration.application.IntegrationEventProcessor
import network.marsys.smarthome.hub.feature.integration.application.IntegrationLifecycleManager
import org.koin.ktor.ext.inject

private val logger = KotlinLogging.logger { }

@Suppress("FunctionNameMaxLength")
fun Application.initializeIntegrationEventProcessor() {
    val integrationEventProcessor by inject<IntegrationEventProcessor>()
    val integrationLifecycleManager by inject<IntegrationLifecycleManager>()

    val applicationStartedHandler = monitor.subscribe(ApplicationStarted) {
        logger.info { "Starting integration event processor." }

        integrationLifecycleManager.all().forEach { integration ->
            launch {
                integration.events.collect { event ->
                    logger.debug { "Received event '${event::class.simpleName}' for entity '${event.identifier}'" }
                    integrationEventProcessor.process(event)
                }
            }
        }
    }

    val applicationStoppingHandler = monitor.subscribe(ApplicationStopping) {
        logger.info { "Stopping integration event processor." }
        applicationStartedHandler.dispose()
    }

    monitor.subscribe(ApplicationStopped) {
        applicationStoppingHandler.dispose()
    }
}
