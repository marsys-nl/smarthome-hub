package network.marsys.smarthome.hub.feature.entity.application.di

import network.marsys.smarthome.hub.feature.entity.application.internal.EntityEventProcessor
import network.marsys.smarthome.hub.feature.entity.application.ports.inbound.ProcessEntityEvent
import org.koin.dsl.module

fun entityApplicationModule() = module {
    single<ProcessEntityEvent> {
        EntityEventProcessor(
            store = get(),
            publisher = get(),
        )
    }
}
