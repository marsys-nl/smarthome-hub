package network.marsys.smarthome.hub.plugin

import io.ktor.server.application.Application
import io.ktor.server.application.install
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonBuilder
import kotlinx.serialization.modules.SerializersModule
import network.marsys.smarthome.api.apiModuleSerializersModule
import network.marsys.smarthome.hub.feature.entity.infrastructure.di.entityFeatureModule
import network.marsys.smarthome.hub.feature.integration.application.IntegrationLifecycleManager
import network.marsys.smarthome.hub.feature.integration.application.ports.inbound.IntegrationQueries
import network.marsys.smarthome.hub.feature.integration.application.ports.inbound.ManageIntegrationLifecycle
import network.marsys.smarthome.hub.feature.integration.infrastructure.FakeIntegrationAdapter
import network.marsys.smarthome.hub.feature.integration.infrastructure.SystemInfoIntegrationAdapter
import network.marsys.smarthome.hub.feature.platform.infrastructure.di.platformFeatureModule
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin

fun Application.initializeDependencyInjection() {
    install(Koin) {
        modules(
            applicationModule,

            entityFeatureModule(),
            integrationFeatureModule,
            platformFeatureModule(),
        )
    }
}

internal val applicationModule = module {
    factory<Json> { (builder: JsonBuilder.() -> Unit) ->
        Json {
            builder.invoke(this)

            serializersModule = SerializersModule {
                include(apiModuleSerializersModule)
            }
        }
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
}
