package network.marsys.smarthome.hub.plugin

import io.ktor.server.application.Application
import io.ktor.server.application.install
import network.marsys.smarthome.hub.core.eventstore.application.ports.outbound.EventStore
import network.marsys.smarthome.hub.core.eventstore.infrastructure.InMemoryEventStore
import network.marsys.smarthome.hub.feature.entity.application.EntityAggregate
import network.marsys.smarthome.hub.feature.entity.application.usecase.GetEntities
import network.marsys.smarthome.hub.feature.integration.application.IntegrationEventProcessor
import network.marsys.smarthome.hub.feature.integration.application.IntegrationLifecycleManager
import network.marsys.smarthome.hub.feature.integration.application.ports.inbound.IntegrationQueries
import network.marsys.smarthome.hub.feature.integration.application.ports.inbound.ManageIntegrationLifecycle
import network.marsys.smarthome.hub.feature.integration.infrastructure.FakeIntegrationAdapter
import network.marsys.smarthome.hub.feature.integration.infrastructure.SystemInfoIntegrationAdapter
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin

fun Application.initializeDependencyInjection() {
    install(Koin) {
        modules(
            coreEventStoreModule,
            entityFeatureModule,
            integrationFeatureModule,
        )
    }
}

private val integrationFeatureModule = module {
    val integrationLifecycleManager = IntegrationLifecycleManager(
        integrations = listOf(
            SystemInfoIntegrationAdapter(),
            FakeIntegrationAdapter(),
        ),
    )

    single<IntegrationLifecycleManager> { integrationLifecycleManager }
    single<IntegrationQueries> { integrationLifecycleManager }
    single<ManageIntegrationLifecycle> { integrationLifecycleManager }

    single<IntegrationEventProcessor> { IntegrationEventProcessor(eventStore = get()) }
}

private val entityFeatureModule = module {
    single {
        val eventStore: EventStore = get()

        GetEntities {
            eventStore.identifiers()
                .map { identifier -> eventStore.load(entity = identifier) }
                .map { history -> EntityAggregate(history = history) }
                .map { aggregate -> aggregate.entity }
        }
    }
}

private val coreEventStoreModule = module {
    single<EventStore> { InMemoryEventStore() }
}
