package network.marsys.smarthome.hub

import de.infix.testBalloon.framework.core.testSuite
import dev.nmarsman.expect.api.expectThat
import dev.nmarsman.expect.assertions.containsExactly
import dev.nmarsman.expect.assertions.filter
import dev.nmarsman.expect.assertions.isA
import dev.nmarsman.expect.assertions.isEmpty
import dev.nmarsman.expect.assertions.isEqualTo
import dev.nmarsman.expect.assertions.isFalse
import dev.nmarsman.expect.assertions.isTrue
import dev.nmarsman.expect.assertions.single
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import network.marsys.smarthome.domain.TranslationKey
import network.marsys.smarthome.domain.capability.Capability
import network.marsys.smarthome.domain.capability.Capability.Companion.optional
import network.marsys.smarthome.domain.capability.Capability.Companion.required
import network.marsys.smarthome.domain.capability.OnOff
import network.marsys.smarthome.domain.entity.EntityIdentifier
import network.marsys.smarthome.domain.entity.Light
import network.marsys.smarthome.domain.entity.naming
import network.marsys.smarthome.domain.identifiers.IntegrationIdentifier
import network.marsys.smarthome.hub.feature.entity.application.ports.inbound.ExecuteEntityAction
import network.marsys.smarthome.hub.feature.entity.application.ports.inbound.ProcessEntityEvent
import network.marsys.smarthome.hub.feature.entity.application.ports.outbound.EventStore
import network.marsys.smarthome.hub.feature.entity.domain.EntityAggregate
import network.marsys.smarthome.hub.feature.entity.domain.event.CapabilityUpdated
import network.marsys.smarthome.hub.feature.entity.domain.event.EntityActionAccepted
import network.marsys.smarthome.hub.feature.entity.domain.event.EntityActionCompleted
import network.marsys.smarthome.hub.feature.entity.domain.event.EntityDiscovered
import network.marsys.smarthome.hub.feature.entity.domain.event.EntityProvisioned
import network.marsys.smarthome.hub.feature.entity.domain.event.Event
import network.marsys.smarthome.hub.feature.entity.infrastructure.di.entityFeatureModule
import network.marsys.smarthome.hub.feature.integration.application.IntegrationAdapter
import network.marsys.smarthome.hub.feature.integration.application.di.integrationApplicationModule
import network.marsys.smarthome.hub.feature.integration.application.ports.inbound.IntegrationActionExecutor
import network.marsys.smarthome.hub.feature.integration.application.ports.inbound.IntegrationQueries
import network.marsys.smarthome.hub.feature.integration.application.ports.outbound.EntityEventPublisher
import network.marsys.smarthome.hub.feature.integration.domain.Integration
import network.marsys.smarthome.hub.feature.platform.infrastructure.di.platformFeatureModule
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

val EntityActionExecutionIntegrationTest by testSuite(
    name = "Entity action execution integration test",
) {
    test(name = "Given a discovered light entity that is off, when toggled on, then the on action should be executed") {
        runTest {
            val identifier = EntityIdentifier("light.kitchen")

            val light = Light(
                identifier = identifier,
                naming = Light.naming(
                    key = TranslationKey("light.kitchen"),
                ),
                state = Light.State.Known(
                    onOff = required(OnOff(current = false)),
                    brightness = optional(null),
                ),
            )

            val now = Clock.System.now()

            val provisioned = EntityProvisioned(
                identifier = identifier,
                type = light.type,
                naming = light.naming,
                occurredAt = now,
            )

            val discovered = EntityDiscovered(
                identifier = identifier,
                state = light.state,
                occurredAt = now,
            )

            val publisher = FakeEntityEventPublisher()
            val integration = TestActionIntegrationAdapter(
                entity = identifier,
                publisher = publisher,
            )

            val application = koinApplication {
                modules(
                    entityFeatureModule(),
                    integrationApplicationModule(),
                    platformFeatureModule(),

                    module {
                        single<IntegrationQueries> {
                            IntegrationQueries {
                                listOf(integration)
                            }
                        }
                    },
                )
            }

            val execute: ExecuteEntityAction = application.koin.get()
            val processor: ProcessEntityEvent = application.koin.get()
            val store: EventStore = application.koin.get()

            // Consume integration events
            val consumer = backgroundScope.launch(
                start = CoroutineStart.UNDISPATCHED,
            ) {
                publisher.events.collect { event ->
                    processor.invoke(event = event)
                }
            }

            try {
                // Given: The entity is provisioned and discovered
                processor.invoke(provisioned)
                processor.invoke(discovered)

                expectThat(store.load(entity = identifier))
                    .containsExactly(provisioned, discovered)

                val initial = EntityAggregate(history = store.load(entity = identifier))

                // Given: The light entity is currently off
                expectThat(initial.entity)
                    .isA<Light>()
                    .get(Light::state)
                    .isA<Light.State.Known>()
                    .get(Light.State.Known::onOff)
                    .get(Capability.Required<OnOff>::value)
                    .get(OnOff::current)
                    .isFalse()

                // When: Request to turn on the light
                execute.invoke(
                    OnOff.Toggle.On(
                        entity = identifier,
                    ),
                )

                awaitEvent(
                    store = store,
                    entity = identifier,
                    condition = { history ->
                        history.any { it is EntityActionCompleted }
                    },
                )

                // Then: Reconstruct the aggregate
                val history = store.load(entity = initial.entity.identifier)
                val aggregate = EntityAggregate(history = history)

                // Then: The aggregate should have the light turned on
                expectThat(aggregate.entity)
                    .isA<Light>()
                    .get(Light::state)
                    .isA<Light.State.Known>()
                    .get(Light.State.Known::onOff)
                    .get(Capability.Required<OnOff>::value)
                    .get(OnOff::current)
                    .isTrue()

                // Then: The aggregate should have no pending actions
                expectThat(aggregate.pending)
                    .isEmpty()

                // Then: The history should contain an EntityActionAccepted event
                expectThat(history)
                    .filter { it is EntityActionAccepted }
                    .single()
                    .isA<EntityActionAccepted>()
                    .get(EntityActionAccepted::identifier)
                    .isEqualTo(identifier)

                // Then: The history should contain an EntityActionCompleted event
                expectThat(history)
                    .filter { it is EntityActionCompleted }
                    .single()
                    .isA<EntityActionCompleted>()
                    .get(EntityActionCompleted::identifier)
                    .isEqualTo(identifier)
            } finally {
                consumer.cancelAndJoin()
                integration.stop()
            }
        }
    }
}

private suspend fun awaitEvent(
    store: EventStore,
    entity: EntityIdentifier,
    condition: (Collection<Event>) -> Boolean,
    timeout: Duration = 5.seconds,
    delay: Duration = 100.milliseconds,
) {
    withTimeout(timeout = timeout) {
        while (!condition.invoke(store.load(entity = entity))) {
            delay(delay)
        }
    }
}
private class TestActionIntegrationAdapter(
    private val entity: EntityIdentifier,
    private val publisher: EntityEventPublisher,
) : IntegrationAdapter, IntegrationActionExecutor {

    override val identifier =
        IntegrationIdentifier("integration.test-action")

    override val status: StateFlow<Integration.Status> =
        MutableStateFlow(Integration.Status.Running)

    override suspend fun start() = Unit
    override suspend fun stop() = Unit
    override suspend fun restart() = Unit

    override suspend fun execute(
        action: EntityActionAccepted,
    ): IntegrationActionExecutor.Result {
        if (action.identifier != entity) {
            return IntegrationActionExecutor.Result.NotResponsible
        }

        return when (action.action) {
            is OnOff.Toggle.On -> {
                publisher.publish(
                    CapabilityUpdated(
                        identifier = entity,
                        capability = OnOff(current = true),
                        occurredAt = Clock.System.now(),
                    ),
                )

                publisher.publish(
                    EntityActionCompleted(
                        accepted = action,
                        occurredAt = Clock.System.now(),
                    ),
                )

                IntegrationActionExecutor.Result.Claimed
            }

            else ->
                IntegrationActionExecutor.Result.NotResponsible
        }
    }
}

internal class FakeEntityEventPublisher(
    private val processor: suspend (Event) -> EntityEventPublisher.Result = {
        EntityEventPublisher.Result.Accepted
    },
) : EntityEventPublisher {

    private val channel = Channel<Event>(Channel.UNLIMITED)
    val events: Flow<Event> = channel.receiveAsFlow()

    override suspend fun publish(
        event: Event,
    ): EntityEventPublisher.Result =
        processor(event).also {
            channel.send(element = event)
        }
}
