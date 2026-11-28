package network.marsys.smarthome.hub.feature.entity.infrastructure.di

import network.marsys.smarthome.hub.feature.entity.application.di.entityApplicationModule
import network.marsys.smarthome.hub.feature.entity.application.ports.inbound.FindEntity
import network.marsys.smarthome.hub.feature.entity.application.ports.inbound.GetEntities
import network.marsys.smarthome.hub.feature.entity.application.ports.outbound.EntityUpdatePublisher
import network.marsys.smarthome.hub.feature.entity.application.ports.outbound.EventStore
import network.marsys.smarthome.hub.feature.entity.infrastructure.InMemoryEventStore
import network.marsys.smarthome.hub.feature.entity.infrastructure.PlatformEntityUpdatePublisher
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.binds
import org.koin.dsl.module

fun entityFeatureModule(): Module = module {
    singleOf(::InMemoryEventStore) binds arrayOf(EventStore::class, FindEntity::class, GetEntities::class)
    singleOf(::PlatformEntityUpdatePublisher) bind EntityUpdatePublisher::class

    includes(entityApplicationModule())
}
