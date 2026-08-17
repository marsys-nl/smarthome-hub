package network.marsys.smarthome.hub.feature.integration.infrastructure.fake

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import network.marsys.smarthome.domain.identifiers.EntityIdentifier
import network.marsys.smarthome.domain.identifiers.IntegrationIdentifier
import network.marsys.smarthome.hub.feature.entity.domain.entity.Light
import network.marsys.smarthome.hub.feature.entity.domain.event.EntityProvisioned
import network.marsys.smarthome.hub.feature.entity.domain.event.Event
import network.marsys.smarthome.hub.feature.integration.application.IntegrationAdapter
import network.marsys.smarthome.hub.feature.integration.application.IntegrationLifecycleController
import network.marsys.smarthome.hub.feature.integration.domain.Integration
import kotlin.time.Duration.Companion.seconds

class FakeIntegrationAdapter(
    override val identifier: IntegrationIdentifier = IntegrationIdentifier("integration.fake"),
    initialStatus: Integration.Status = Integration.Status.Stopped,
) : IntegrationAdapter {
    private val entityIdentifier = EntityIdentifier("entity.fake")

    private val lifecycle = IntegrationLifecycleController(
        initialStatus = initialStatus,
        onStart = {
            delay(2.seconds)

            eventChannel.trySend(
                EntityProvisioned(
                    identifier = entityIdentifier,
                    type = Light,
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
