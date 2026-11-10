package network.marsys.smarthome.hub.feature.platform.infrastructure.di

import network.marsys.smarthome.hub.feature.platform.application.PlatformEventPublisher
import network.marsys.smarthome.hub.feature.platform.application.PlatformEventStream
import network.marsys.smarthome.hub.feature.platform.infrastructure.InMemoryPlatformEventBus
import org.koin.core.module.Module
import org.koin.dsl.binds
import org.koin.dsl.module

fun platformFeatureModule(): Module = module {
    single {
        InMemoryPlatformEventBus()
    } binds arrayOf(
        InMemoryPlatformEventBus::class,
        PlatformEventPublisher::class,
        PlatformEventStream::class,
    )
}
