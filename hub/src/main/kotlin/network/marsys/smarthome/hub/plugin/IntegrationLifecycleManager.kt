package network.marsys.smarthome.hub.plugin

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStarted
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.application.ApplicationStopping
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import network.marsys.smarthome.hub.feature.integration.application.IntegrationLifecycleManager
import org.koin.ktor.ext.inject

private val logger = KotlinLogging.logger { }

@Suppress("FunctionNameMaxLength")
fun Application.initializeIntegrationLifecycleManager() {
    val integrationLifecycleManager by inject<IntegrationLifecycleManager>()

    val applicationStartedHandler = monitor.subscribe(ApplicationStarted) {
        launch {
            logger.info { "Starting integration lifecycle manager..." }
            integrationLifecycleManager.start()
        }
    }

    val applicationStoppingHandler = monitor.subscribe(ApplicationStopping) {
        runBlocking {
            logger.info { "Application is preparing to stop, shutting down integration lifecycle manager..." }
            integrationLifecycleManager.stop()
        }
    }

    monitor.subscribe(ApplicationStopped) {
        logger.info { "Application is stopped." }

        applicationStartedHandler.dispose()
        applicationStoppingHandler.dispose()
    }
}
