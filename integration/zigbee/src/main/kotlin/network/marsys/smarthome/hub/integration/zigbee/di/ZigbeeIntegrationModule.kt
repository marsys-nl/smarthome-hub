

package network.marsys.smarthome.hub.integration.zigbee.di

import network.marsys.smarthome.domain.identifiers.IntegrationIdentifier
import network.marsys.smarthome.hub.feature.integration.application.IntegrationAdapter
import network.marsys.smarthome.hub.infrastructure.mqtt.MqttClient
import network.marsys.smarthome.hub.integration.zigbee.ZigbeeIntegrationAdapter
import org.koin.core.qualifier.named
import org.koin.dsl.binds
import org.koin.dsl.module

fun zigbeeIntegrationModule() = module {
    single<IntegrationAdapter>(qualifier = named("integration.zigbee")) {
        ZigbeeIntegrationAdapter(
            identifier = IntegrationIdentifier("integration.zigbee"),
            client = get<MqttClient>(),
            scope = get(named("integrationCoroutineScope")),
        )
    } binds arrayOf(
        ZigbeeIntegrationAdapter::class,
    )
}
