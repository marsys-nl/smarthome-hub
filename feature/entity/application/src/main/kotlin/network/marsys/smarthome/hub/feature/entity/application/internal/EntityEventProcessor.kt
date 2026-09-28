package network.marsys.smarthome.hub.feature.entity.application.internal

import io.github.oshai.kotlinlogging.KotlinLogging
import network.marsys.smarthome.hub.feature.entity.application.EntityAggregate
import network.marsys.smarthome.hub.feature.entity.application.ports.inbound.ProcessEntityEvent
import network.marsys.smarthome.hub.feature.entity.application.ports.outbound.EventStore
import network.marsys.smarthome.hub.feature.entity.domain.entity.Entity
import network.marsys.smarthome.hub.feature.entity.domain.event.CapabilityUpdated
import network.marsys.smarthome.hub.feature.entity.domain.event.EntityBecameUnavailable
import network.marsys.smarthome.hub.feature.entity.domain.event.EntityDiscovered
import network.marsys.smarthome.hub.feature.entity.domain.event.EntityProvisioned
import network.marsys.smarthome.hub.feature.entity.domain.event.Event

private val logger = KotlinLogging.logger {}

fun processEntityEvent(
    store: EventStore,
): ProcessEntityEvent = EntityEventProcessor(store)

internal class EntityEventProcessor(
    private val store: EventStore,
) : ProcessEntityEvent {
    override suspend fun invoke(event: Event): ProcessEntityEvent.Result {
        val history = store.load(entity = event.identifier)

        return process(event = event, history = history)
            .also { result ->
                when (result) {
                    is ProcessEntityEvent.Result.Accepted ->
                        store.append(event)

                    is ProcessEntityEvent.Result.Ignored ->
                        logger.debug {
                            "Event '${event::class.simpleName}' for entity '${event.identifier}' " +
                                "was ${result::class.simpleName}."
                        }

                    is ProcessEntityEvent.Result.Rejected ->
                        logger.warn {
                            "Event '${event::class.simpleName}' for entity '${event.identifier}' " +
                                "was ${result::class.simpleName}."
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
                is EntityProvisioned -> ProcessEntityEvent.Result.Rejected(
                    reason = ProcessEntityEvent.Result.Rejected.Reason.AlreadyProvisioned,
                )

                is EntityDiscovered ->
                    processEntityDiscovered()

                is EntityBecameUnavailable ->
                    processEntityBecameUnavailable()

                is CapabilityUpdated -> context(with = event) {
                    processCapabilityUpdated()
                }
            }
        }
    } catch (_: IllegalStateException) {
        ProcessEntityEvent.Result.Ignored
    }

    context(_: EntityAggregate)
    private fun processEntityDiscovered(): ProcessEntityEvent.Result =
        ProcessEntityEvent.Result.Accepted

    context(aggregate: EntityAggregate)
    private fun processEntityBecameUnavailable(): ProcessEntityEvent.Result = when (aggregate.entity.state) {
        is Entity.State.Unknown -> ProcessEntityEvent.Result.Ignored
        is Entity.State.Known -> ProcessEntityEvent.Result.Accepted
    }

    context(aggregate: EntityAggregate, event: CapabilityUpdated)
    private fun processCapabilityUpdated(): ProcessEntityEvent.Result = when (aggregate.entity.state) {
        is Entity.State.Unknown -> ProcessEntityEvent.Result.Rejected(
            reason = ProcessEntityEvent.Result.Rejected.Reason.NotDiscovered,
        )

        is Entity.State.Known -> {
            val capability = aggregate.entity.state.get(event.capability) ?: return ProcessEntityEvent.Result.Rejected(
                reason = ProcessEntityEvent.Result.Rejected.Reason.CapabilityNotPresent,
            )

            return when {
                capability.current == event.capability.current -> ProcessEntityEvent.Result.Ignored
                else -> ProcessEntityEvent.Result.Accepted
            }
        }
    }
}
