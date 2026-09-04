package network.marsys.smarthome.hub.plugin

import io.ktor.server.application.Application
import io.ktor.server.application.install
import network.marsys.smarthome.hub.feature.integration.application.IntegrationEventProcessor
import network.marsys.smarthome.hub.feature.integration.application.IntegrationLifecycleManager
import network.marsys.smarthome.hub.feature.integration.application.ports.inbound.IntegrationQueries
import network.marsys.smarthome.hub.feature.integration.application.ports.inbound.ManageIntegrationLifecycle
import network.marsys.smarthome.hub.feature.integration.application.ports.outbound.EventStore
import network.marsys.smarthome.hub.feature.integration.infrastructure.FakeIntegrationAdapter
import network.marsys.smarthome.hub.feature.integration.infrastructure.InMemoryEventStore
import network.marsys.smarthome.hub.feature.integration.infrastructure.SystemInfoIntegrationAdapter
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin

fun Application.initializeDependencyInjection() {
    install(Koin) {
        modules(
            integrationLifecycleManagerModule,
        )
    }
}

private val integrationLifecycleManagerModule = module {
    val integrationLifecycleManager = IntegrationLifecycleManager(
        integrations = listOf(
            SystemInfoIntegrationAdapter(),
            FakeIntegrationAdapter(),
        ),
    )

    single<IntegrationLifecycleManager> { integrationLifecycleManager }
    single<IntegrationQueries> { integrationLifecycleManager }
    single<ManageIntegrationLifecycle> { integrationLifecycleManager }

    single<EventStore> { InMemoryEventStore() }

    single<IntegrationEventProcessor> { IntegrationEventProcessor(eventStore = get()) }
}
