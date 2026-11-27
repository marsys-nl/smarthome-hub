package network.marsys.smarthome.hub.integration.zigbee

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import network.marsys.smarthome.domain.identifiers.IntegrationIdentifier
import network.marsys.smarthome.hub.feature.entity.domain.event.EntityActionAccepted
import network.marsys.smarthome.hub.feature.entity.domain.event.Event
import network.marsys.smarthome.hub.feature.integration.application.IntegrationAdapter
import network.marsys.smarthome.hub.feature.integration.application.IntegrationLifecycleController
import network.marsys.smarthome.hub.feature.integration.application.ports.inbound.IntegrationActionExecutor
import network.marsys.smarthome.hub.feature.integration.domain.Integration
import network.marsys.smarthome.hub.infrastructure.mqtt.MqttClient

class ZigbeeIntegrationAdapter(
    override val identifier: IntegrationIdentifier = IntegrationIdentifier("integration.zigbee"),
    private val client: MqttClient,
    scope: CoroutineScope,
) : IntegrationAdapter, IntegrationActionExecutor {
    private val logger = KotlinLogging.logger { }

    private val lifecycle = IntegrationLifecycleController(
        scope = scope,
        initialStatus = Integration.Status.Stopped,
        onStart = {
            launch {
                println("Starting zigbee")

                client.subscribe("zigbee2mqtt/#")
                    .collect { message ->
                        logger.info {
                            "Received message on topic: ${message.topic}, " +
                                "payload: ${message.payload.toString(Charsets.UTF_8)}"
                        }
                    }
            }
        },
        onStop = {},
    )

    private val eventChannel = Channel<Event>(Channel.BUFFERED)
    override val events: Flow<Event> = eventChannel.receiveAsFlow()

    override val status: StateFlow<Integration.Status> = lifecycle.status

    override suspend fun execute(action: EntityActionAccepted): IntegrationActionExecutor.Result =
        IntegrationActionExecutor.Result.NotResponsible

    override suspend fun start() = lifecycle.start()
    override suspend fun stop() = lifecycle.stop()
    override suspend fun restart() = lifecycle.restart()
}
