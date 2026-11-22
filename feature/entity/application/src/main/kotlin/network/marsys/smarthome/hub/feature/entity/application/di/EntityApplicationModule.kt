package network.marsys.smarthome.hub.feature.entity.application.di

import network.marsys.smarthome.hub.feature.entity.application.internal.EntityEventProcessor
import network.marsys.smarthome.hub.feature.entity.application.internal.EntitySynchronization
import network.marsys.smarthome.hub.feature.entity.application.internal.ExecuteEntityActionImpl
import network.marsys.smarthome.hub.feature.entity.application.ports.inbound.ExecuteEntityAction
import network.marsys.smarthome.hub.feature.entity.application.ports.inbound.ProcessEntityEvent
import org.koin.dsl.binds
import org.koin.dsl.module

fun entityApplicationModule() = module {
    single {
        EntitySynchronization()
    } binds arrayOf(
        EntitySynchronization::class,
    )

    single {
        ExecuteEntityActionImpl(
            store = get(),
            synchronization = get(),
        )
    } binds arrayOf(
        ExecuteEntityAction::class,
    )

    single {
        EntityEventProcessor(
            store = get(),
            publisher = get(),
            synchronization = get(),
        )
    } binds arrayOf(
        ProcessEntityEvent::class,
    )
}
