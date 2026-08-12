package network.marsys.smarthome.hub.plugin

import io.ktor.server.application.Application
import io.ktor.server.application.install
import network.marsys.smarthome.hub.feature.integration.application.IntegrationLifecycleManager
import network.marsys.smarthome.hub.feature.integration.application.ports.inbound.IntegrationQueries
import network.marsys.smarthome.hub.feature.integration.application.ports.inbound.ManageIntegrationLifecycle
import network.marsys.smarthome.hub.feature.integration.infrastructure.fake.FakeIntegrationAdapter
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
            FakeIntegrationAdapter(),
        ),
    )

    single<IntegrationLifecycleManager> { integrationLifecycleManager }
    single<IntegrationQueries> { integrationLifecycleManager }
    single<ManageIntegrationLifecycle> { integrationLifecycleManager }
}
