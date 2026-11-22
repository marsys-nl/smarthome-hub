package network.marsys.smarthome.hub.feature.entity.application.internal

import io.github.oshai.kotlinlogging.KotlinLogging
import network.marsys.smarthome.domain.capability.with
import network.marsys.smarthome.domain.entity.Entity
import network.marsys.smarthome.hub.feature.entity.application.ports.inbound.ProcessEntityEvent
import network.marsys.smarthome.hub.feature.entity.application.ports.outbound.EntityUpdatePublisher
import network.marsys.smarthome.hub.feature.entity.application.ports.outbound.EventStore
import network.marsys.smarthome.hub.feature.entity.domain.EntityAggregate
import network.marsys.smarthome.hub.feature.entity.domain.event.CapabilityUpdated
import network.marsys.smarthome.hub.feature.entity.domain.event.EntityActionAccepted
import network.marsys.smarthome.hub.feature.entity.domain.event.EntityActionFinished
import network.marsys.smarthome.hub.feature.entity.domain.event.EntityBecameUnavailable
import network.marsys.smarthome.hub.feature.entity.domain.event.EntityDiscovered
import network.marsys.smarthome.hub.feature.entity.domain.event.EntityProvisioned
import network.marsys.smarthome.hub.feature.entity.domain.event.Event

private val logger = KotlinLogging.logger {}

internal class EntityEventProcessor(
    private val store: EventStore,
    private val publisher: EntityUpdatePublisher,
    private val synchronization: EntitySynchronization,
) : ProcessEntityEvent {
    override suspend fun invoke(event: Event): ProcessEntityEvent.Result =
        synchronization.withLock(entity = event.identifier) {
            val history = store.load(entity = event.identifier)

            process(event = event, history = history)
                .also { result ->
                    when (result) {
                        is ProcessEntityEvent.Result.Accepted -> {
                            store.append(event)

                            val updated = EntityAggregate(
                                history = history + event,
                            )

                            publisher.publish(
                                event = event,
                                entity = updated.entity,
                            )
                        }

                        is ProcessEntityEvent.Result.Ignored -> {
                            logger.debug {
                                "Event '${event::class.simpleName}' for entity '${event.identifier}' " +
                                    "was ${result::class.simpleName}."
                            }
                        }

                        is ProcessEntityEvent.Result.Rejected -> {
                            logger.warn {
                                "Event '${event::class.simpleName}' for entity '${event.identifier}' " +
                                    "was ${result::class.simpleName}."
                            }
                        }
                    }
                }
        }

    private fun process(event: Event, history: Collection<Event>): ProcessEntityEvent.Result = try {
        val aggregate = EntityAggregate(
            history = history.takeIf { it.isNotEmpty() }
                ?: return when (event) {
                    is EntityProvisioned -> ProcessEntityEvent.Result.Accepted

                    else -> ProcessEntityEvent.Result.Rejected(
                        reason = ProcessEntityEvent.Result.Rejected.Reason.NotProvisioned,
                    )
                },
        )

        return context(with = aggregate) {
            when (event) {
                is CapabilityUpdated -> context(with = event) {
                    processCapabilityUpdated()
                }

                is EntityActionAccepted -> ProcessEntityEvent.Result.Ignored

                is EntityActionFinished -> context(with = event) {
                    processEntityActionEvent()
                }

                is EntityBecameUnavailable ->
                    processEntityBecameUnavailable()

                is EntityDiscovered ->
                    processEntityDiscovered()

                is EntityProvisioned -> ProcessEntityEvent.Result.Rejected(
                    reason = ProcessEntityEvent.Result.Rejected.Reason.AlreadyProvisioned,
                )
            }
        }
    } catch (_: IllegalStateException) {
        ProcessEntityEvent.Result.Ignored
    }

    context(aggregate: EntityAggregate, event: CapabilityUpdated)
    private fun processCapabilityUpdated(): ProcessEntityEvent.Result = when (aggregate.entity.state) {
        is Entity.State.Unknown -> ProcessEntityEvent.Result.Rejected(
            reason = ProcessEntityEvent.Result.Rejected.Reason.NotDiscovered,
        )

        is Entity.State.Known -> {
            val capability = aggregate.entity.state.get(event.capability.type with event.capability.context)
                ?: return ProcessEntityEvent.Result.Rejected(
                    reason = ProcessEntityEvent.Result.Rejected.Reason.CapabilityNotPresent,
                )

            return when {
                capability.current == event.capability.current -> ProcessEntityEvent.Result.Ignored
                else -> ProcessEntityEvent.Result.Accepted
            }
        }
    }

    context(aggregate: EntityAggregate, event: EntityActionFinished)
    private fun processEntityActionEvent(): ProcessEntityEvent.Result = when {
        aggregate.pending.any {
            it.action == event.accepted.action &&
                it.acceptedAt == event.accepted.occurredAt
        } -> ProcessEntityEvent.Result.Accepted

        else -> ProcessEntityEvent.Result.Ignored
    }

    context(_: EntityAggregate)
    private fun processEntityDiscovered(): ProcessEntityEvent.Result =
        ProcessEntityEvent.Result.Accepted

    context(aggregate: EntityAggregate)
    private fun processEntityBecameUnavailable(): ProcessEntityEvent.Result = when (aggregate.entity.state) {
        is Entity.State.Unknown -> ProcessEntityEvent.Result.Ignored
        is Entity.State.Known -> ProcessEntityEvent.Result.Accepted
    }
}
