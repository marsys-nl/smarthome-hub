package network.marsys.smarthome.hub.integration.zigbee

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import network.marsys.smarthome.domain.TranslationKey
import network.marsys.smarthome.domain.entity.Entity
import network.marsys.smarthome.domain.entity.EntityIdentifier
import network.marsys.smarthome.domain.entity.EntityNaming
import network.marsys.smarthome.domain.entity.Light
import network.marsys.smarthome.domain.entity.naming
import network.marsys.smarthome.domain.identifiers.IntegrationIdentifier
import network.marsys.smarthome.hub.common.annotations.ExcludeFromCoverage
import network.marsys.smarthome.hub.feature.entity.domain.event.EntityActionAccepted
import network.marsys.smarthome.hub.feature.entity.domain.event.EntityDiscovered
import network.marsys.smarthome.hub.feature.entity.domain.event.EntityProvisioned
import network.marsys.smarthome.hub.feature.entity.domain.event.Event
import network.marsys.smarthome.hub.feature.integration.application.IntegrationAdapter
import network.marsys.smarthome.hub.feature.integration.application.IntegrationLifecycleController
import network.marsys.smarthome.hub.feature.integration.application.ports.inbound.IntegrationActionExecutor
import network.marsys.smarthome.hub.feature.integration.application.ports.outbound.EntityEventPublisher
import network.marsys.smarthome.hub.feature.integration.application.ports.outbound.EntityQuery
import network.marsys.smarthome.hub.feature.integration.domain.Integration
import network.marsys.smarthome.hub.infrastructure.mqtt.Message
import network.marsys.smarthome.hub.infrastructure.mqtt.MqttClient
import network.marsys.smarthome.hub.integration.zigbee.ZigbeeIntegrationAdapter.Companion.BASE_TOPIC
import network.marsys.smarthome.hub.integration.zigbee.api.ZigbeeDevice
import network.marsys.smarthome.hub.integration.zigbee.api.ZigbeeDeviceState
import network.marsys.smarthome.hub.integration.zigbee.internal.ZigbeeDeviceRegistry
import network.marsys.smarthome.hub.integration.zigbee.internal.ZigbeeDeviceStateRegistry
import network.marsys.smarthome.hub.integration.zigbee.internal.ZigbeeEntityRecognizer
import network.marsys.smarthome.hub.integration.zigbee.internal.mapper.state.ZigbeeEntityStateMapper
import network.marsys.smarthome.hub.integration.zigbee.internal.mapper.state.ZigbeeLightStateMapper
import kotlin.time.Clock

class ZigbeeIntegrationAdapter(
    override val identifier: IntegrationIdentifier = IntegrationIdentifier("integration.zigbee"),
    private val client: MqttClient,
    private val publisher: EntityEventPublisher,
    private val query: EntityQuery,
    private val clock: Clock,
    scope: CoroutineScope,
) : IntegrationAdapter, IntegrationActionExecutor {
    private val logger = KotlinLogging.logger { }

    private val json = Json {
        ignoreUnknownKeys = true
    }

    private val entityRecognizer = ZigbeeEntityRecognizer()
    private val deviceRegistry = ZigbeeDeviceRegistry()
    private val deviceStateRegistry = ZigbeeDeviceStateRegistry()

    private val lifecycle = IntegrationLifecycleController(
        scope = scope,
        initialStatus = Integration.Status.Stopped,
        onStart = {
            deviceRegistry.reconcile(snapshot = emptyList())
            deviceStateRegistry.clear()

            launch {
                client.subscribe("zigbee2mqtt/#")
                    .collect { process(it) }
            }
        },
        onStop = {},
    )

    override val status: StateFlow<Integration.Status> = lifecycle.status

    @ExcludeFromCoverage(reason = "Not yet used in production code")
    override suspend fun execute(action: EntityActionAccepted): IntegrationActionExecutor.Result =
        IntegrationActionExecutor.Result.NotResponsible

    override suspend fun start() = lifecycle.start()
    override suspend fun stop() = lifecycle.stop()
    override suspend fun restart() = lifecycle.restart()

    private suspend fun process(message: Message) {
        when (message.topic) {
            "$BASE_TOPIC/bridge/devices" ->
                processDevices(message)

            else ->
                processPotentialDeviceState(message)
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    private suspend fun processDevices(message: Message) {
        val devices = try {
            json.decodeFromStream<List<ZigbeeDevice>>(stream = message.payload.inputStream())
        } catch (exception: SerializationException) {
            logger.warn(exception) {
                "Failed to deserialize Zigbee devices."
            }
            return
        }

        val changes = deviceRegistry.reconcile(snapshot = devices)

        changes.removed.forEach { device ->
            deviceStateRegistry.remove(ieeeAddress = device.ieeeAddress)
        }

        devices.forEach { device ->
            provisionedEventFor(device = device)?.let { event ->
                publisher.publish(event = event)
            }
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    private suspend fun processPotentialDeviceState(message: Message) {
        val device = deviceRegistry.findByTopic(topic = message.topic)
            ?: return

        val state = try {
            json.decodeFromStream<ZigbeeDeviceState>(stream = message.payload.inputStream())
        } catch (exception: SerializationException) {
            logger.warn(exception) {
                "Failed to deserialize Zigbee device state for device '${device.ieeeAddress}'."
            }
            return
        }

        processDeviceState(device, state)
    }

    private suspend fun processDeviceState(device: ZigbeeDevice, state: ZigbeeDeviceState) {
        val state = deviceStateRegistry.update(
            ieeeAddress = device.ieeeAddress,
            state = state,
        )

        publisher.publish(
            event = discoveredEventFor(
                device = device,
                state = state,
            ) ?: return,
        )
    }

    private fun entityIdentifierOf(device: ZigbeeDevice): EntityIdentifier =
        EntityIdentifier("zigbee.${device.ieeeAddress.lowercase()}")

    private fun entityNamingOf(
        device: ZigbeeDevice,
        type: Entity.Type<*>,
    ): EntityNaming =
        when {
            device.friendlyName.startsWith("light.") ->
                type.naming(
                    key = TranslationKey(device.friendlyName),
                )

            else -> type.naming()
        }

    private suspend fun provisionedEventFor(
        device: ZigbeeDevice,
    ): EntityProvisioned? {
        val type = entityRecognizer.recognize(device)
            ?: return null

        val identifier = entityIdentifierOf(device)

        if (query.findByIdentifier(identifier = identifier) != null) {
            return null
        }

        return EntityProvisioned(
            identifier = identifier,
            type = type,
            naming = entityNamingOf(device, type),
            occurredAt = clock.now(),
        )
    }

    private suspend fun discoveredEventFor(
        device: ZigbeeDevice,
        state: ZigbeeDeviceState,
    ): EntityDiscovered? {
        val type = entityRecognizer.recognize(device)
            ?: return null

        val identifier = entityIdentifierOf(device)

        val entity = query.findByIdentifier(identifier = identifier)
            ?: return null

//        val discovered = when (type) {
//            Light if entity is Light && entity.state is Light.State.Unknown -> ZigbeeLightStateMapper.map(
//                device = device,
//                state = state,
//            )
//
//            else -> return null
//        }

        val discovered = if (entity.state is Light.State.Unknown) {
            ZigbeeLightStateMapper.map(
                device = device,
                state = state,
            )
        } else {
            return null
        }

        return EntityDiscovered(
            identifier = identifier,
            state = discovered ?: return null,
            occurredAt = clock.now(),
        )
    }

    companion object {
        internal const val BASE_TOPIC = "zigbee2mqtt"
    }
}

internal fun ZigbeeDeviceRegistry.findByTopic(topic: String): ZigbeeDevice? {
    val prefix = "${BASE_TOPIC}/"

    if (!topic.startsWith(prefix)) {
        return null
    }

    return findByFriendlyName(
        friendlyName = topic.removePrefix(prefix),
    )
}
