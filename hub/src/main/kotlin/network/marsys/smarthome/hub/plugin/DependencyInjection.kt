@file:ExcludeFromCoverage(reason = "Dependency injection module")

package network.marsys.smarthome.hub.plugin

import io.ktor.server.application.Application
import io.ktor.server.application.install
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonBuilder
import kotlinx.serialization.modules.SerializersModule
import network.marsys.smarthome.api.apiModuleSerializersModule
import network.marsys.smarthome.domain.identifiers.IntegrationIdentifier
import network.marsys.smarthome.hub.common.annotations.ExcludeFromCoverage
import network.marsys.smarthome.hub.feature.entity.infrastructure.di.entityFeatureModule
import network.marsys.smarthome.hub.feature.integration.application.IntegrationAdapter
import network.marsys.smarthome.hub.feature.integration.application.IntegrationLifecycleManager
import network.marsys.smarthome.hub.feature.integration.application.di.integrationApplicationModule
import network.marsys.smarthome.hub.feature.integration.application.ports.inbound.IntegrationQueries
import network.marsys.smarthome.hub.feature.integration.application.ports.inbound.ManageIntegrationLifecycle
import network.marsys.smarthome.hub.feature.integration.infrastructure.FakeIntegrationAdapter
import network.marsys.smarthome.hub.feature.integration.infrastructure.SystemInfoIntegrationAdapter
import network.marsys.smarthome.hub.feature.platform.infrastructure.di.platformFeatureModule
import network.marsys.smarthome.hub.infrastructure.mqtt.MqttClient
import network.marsys.smarthome.hub.infrastructure.mqtt.MqttClientConfiguration
import network.marsys.smarthome.hub.integration.zigbee.di.zigbeeIntegrationModule
import org.koin.core.qualifier.named
import org.koin.dsl.binds
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import kotlin.time.Clock

fun Application.initializeDependencyInjection() {
    install(Koin) {
        modules(
            applicationModule,

            entityFeatureModule(),
            integrationFeatureModule(
                integrationCoroutineScope = this@initializeDependencyInjection,
            ),
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

private fun Application.integrationFeatureModule(
    integrationCoroutineScope: CoroutineScope,
) = module {
    single<Clock> {
        Clock.System
    }

    single<MqttClient> {
        val broker = environment.config.property("smarthome.mqtt.broker.host").getString()
        val username = environment.config.propertyOrNull("smarthome.mqtt.broker.username")?.getString()
        val password = environment.config.propertyOrNull("smarthome.mqtt.broker.password")?.getString()

        MqttClient(
            configuration = MqttClientConfiguration(
                broker = broker,
                client = "smarthome-hub",
                username = username,
                password = password,
            ),
        )
    }

    val coroutineQualifier = named("integrationCoroutineScope")

    single(qualifier = coroutineQualifier) {
        integrationCoroutineScope
    }

    includes(
        module = arrayOf(
            integrationApplicationModule(),
            zigbeeIntegrationModule(),
        ),
    )

    single<IntegrationAdapter>(qualifier = named("integration.fake")) {
        FakeIntegrationAdapter(
            identifier = IntegrationIdentifier("integration.fake"),
            scope = get(coroutineQualifier),
        )
    }

    single<IntegrationAdapter>(qualifier = named("integration.system")) {
        SystemInfoIntegrationAdapter(
            identifier = IntegrationIdentifier("integration.system"),
            scope = get(coroutineQualifier),
        )
    }

    single {
        IntegrationLifecycleManager(
            integrations = getAll<IntegrationAdapter>(),
        )
    } binds arrayOf(
        IntegrationLifecycleManager::class,
        IntegrationQueries::class,
        ManageIntegrationLifecycle::class,
    )
}
