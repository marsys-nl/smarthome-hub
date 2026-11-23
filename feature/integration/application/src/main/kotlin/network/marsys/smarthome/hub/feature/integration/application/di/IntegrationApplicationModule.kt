package network.marsys.smarthome.hub.feature.integration.application.di

import network.marsys.smarthome.hub.feature.entity.application.ports.outbound.EntityActionDispatcher
import network.marsys.smarthome.hub.feature.integration.application.internal.IntegrationEntityActionDispatcher
import org.koin.dsl.binds
import org.koin.dsl.module

fun integrationApplicationModule() = module {
    single {
        IntegrationEntityActionDispatcher(
            executors = getAll(),
        )
    } binds arrayOf(
        EntityActionDispatcher::class,
    )
}
