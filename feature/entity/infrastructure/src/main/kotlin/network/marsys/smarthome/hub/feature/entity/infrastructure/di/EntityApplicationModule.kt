package network.marsys.smarthome.hub.feature.entity.infrastructure.di

import network.marsys.smarthome.hub.feature.entity.application.internal.processEntityEvent
import network.marsys.smarthome.hub.feature.entity.application.ports.inbound.GetEntities
import network.marsys.smarthome.hub.feature.entity.application.ports.inbound.ProcessEntityEvent
import network.marsys.smarthome.hub.feature.entity.application.ports.outbound.EventStore
import network.marsys.smarthome.hub.feature.entity.infrastructure.InMemoryEventStore
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.binds
import org.koin.dsl.module

fun entityFeatureModule(): Module = module {
    singleOf(::InMemoryEventStore) binds arrayOf(EventStore::class, GetEntities::class)

    single<ProcessEntityEvent> {
        processEntityEvent(
            store = get(),
        )
    }
}
