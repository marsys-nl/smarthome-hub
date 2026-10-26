package network.marsys.smarthome.hub.feature.integration.infrastructure

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import network.marsys.smarthome.domain.i18n.TranslationKey
import network.marsys.smarthome.domain.identifiers.EntityIdentifier
import network.marsys.smarthome.domain.identifiers.IntegrationIdentifier
import network.marsys.smarthome.domain.unit.percent
import network.marsys.smarthome.hub.feature.entity.domain.capability.Brightness
import network.marsys.smarthome.hub.feature.entity.domain.capability.Capability.Companion.optional
import network.marsys.smarthome.hub.feature.entity.domain.capability.Capability.Companion.required
import network.marsys.smarthome.hub.feature.entity.domain.capability.OnOff
import network.marsys.smarthome.hub.feature.entity.domain.entity.EntityNaming
import network.marsys.smarthome.hub.feature.entity.domain.entity.Light
import network.marsys.smarthome.hub.feature.entity.domain.entity.naming
import network.marsys.smarthome.hub.feature.entity.domain.event.EntityDiscovered
import network.marsys.smarthome.hub.feature.entity.domain.event.EntityProvisioned
import network.marsys.smarthome.hub.feature.entity.domain.event.Event
import network.marsys.smarthome.hub.feature.integration.application.IntegrationAdapter
import network.marsys.smarthome.hub.feature.integration.application.IntegrationLifecycleController
import network.marsys.smarthome.hub.feature.integration.domain.Integration
import network.marsys.smarthome.hub.feature.integration.domain.entity
import kotlin.time.Duration.Companion.seconds

class FakeIntegrationAdapter(
    override val identifier: IntegrationIdentifier = IntegrationIdentifier("integration.fake"),
    initialStatus: Integration.Status = Integration.Status.Stopped,
) : IntegrationAdapter {
    private val entityIdentifier = identifier.entity("entity")

    private val lifecycle = IntegrationLifecycleController(
        initialStatus = initialStatus,
        onStart = {
            delay(2.seconds)

            eventChannel.trySend(
                EntityProvisioned(
                    identifier = entityIdentifier,
                    type = Light,
                    naming = Light.naming(),
                ),
            )

            eventChannel.trySend(
                EntityDiscovered(
                    identifier = entityIdentifier,
                    state = Light.State.Known(
                        onOff = required(OnOff(current = false)),
                        brightness = optional(Brightness(current = 50.percent)),
                    ),
                ),
            )
        },
        onStop = {
            delay(2.seconds)
        },
    )

    private val eventChannel = Channel<Event>(Channel.BUFFERED)
    override val events: Flow<Event> = eventChannel.receiveAsFlow()

    override val status: StateFlow<Integration.Status> = lifecycle.status

    override suspend fun start() = lifecycle.start()
    override suspend fun stop() = lifecycle.stop()
}
