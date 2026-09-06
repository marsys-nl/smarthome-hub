package network.marsys.smarthome.hub.feature.integration.application

import io.github.oshai.kotlinlogging.KotlinLogging
import network.marsys.smarthome.hub.feature.entity.application.EntityAggregate
import network.marsys.smarthome.hub.feature.entity.domain.entity.Entity
import network.marsys.smarthome.hub.feature.entity.domain.event.CapabilityUpdated
import network.marsys.smarthome.hub.feature.entity.domain.event.EntityBecameUnavailable
import network.marsys.smarthome.hub.feature.entity.domain.event.EntityDiscovered
import network.marsys.smarthome.hub.feature.entity.domain.event.EntityProvisioned
import network.marsys.smarthome.hub.feature.entity.domain.event.Event
import network.marsys.smarthome.hub.feature.integration.application.ports.outbound.EventStore

private val logger = KotlinLogging.logger {}

class IntegrationEventProcessor(
    private val eventStore: EventStore,
) {
    suspend fun process(event: Event): ProcessingResult {
        val history = eventStore.load(entity = event.identifier)

        return process(event = event, history = history)
            .also { result ->
                when (result) {
                    is ProcessingResult.Accepted ->
                        eventStore.append(event)

                    is ProcessingResult.Ignored, is ProcessingResult.Rejected ->
                        logger.warn {
                            "Event '${event::class.simpleName}' for entity '${event.identifier}' " +
                                "was ${result::class.simpleName}."
                        }
                }
            }
    }

    private fun process(event: Event, history: Collection<Event>): ProcessingResult = try {
        val aggregate = EntityAggregate(
            history = history.takeIf { it.isNotEmpty() }
                ?: return when (event) {
                    is EntityProvisioned -> ProcessingResult.Accepted
                    else -> ProcessingResult.Rejected(reason = RejectionReason.NotProvisioned)
                },
        )

        return context(with = aggregate) {
            when (event) {
                is EntityProvisioned -> ProcessingResult.Rejected(
                    reason = RejectionReason.AlreadyProvisioned,
                )

                is EntityDiscovered ->
                    processEntityDiscovered()

                is EntityBecameUnavailable ->
                    processEntityBecameUnavailable()

                is CapabilityUpdated ->
                    processCapabilityUpdated()
            }
        }
    } catch (_: IllegalStateException) {
        ProcessingResult.Ignored
    }

    context(_: EntityAggregate)
    private fun processEntityDiscovered(): ProcessingResult =
        ProcessingResult.Accepted

    context(aggregate: EntityAggregate)
    private fun processEntityBecameUnavailable(): ProcessingResult = when (aggregate.entity.state) {
        is Entity.State.Unknown -> ProcessingResult.Ignored
        is Entity.State.Known -> ProcessingResult.Accepted
    }

    context(aggregate: EntityAggregate)
    private fun processCapabilityUpdated(): ProcessingResult = when (aggregate.entity.state) {
        is Entity.State.Unknown -> ProcessingResult.Rejected(reason = RejectionReason.NotDiscovered)
        is Entity.State.Known -> ProcessingResult.Accepted
    }

    sealed interface ProcessingResult {
        data object Accepted : ProcessingResult
        data object Ignored : ProcessingResult
        data class Rejected(
            val reason: RejectionReason,
        ) : ProcessingResult
    }

    sealed interface RejectionReason {
        data object AlreadyProvisioned : RejectionReason
        data object NotProvisioned : RejectionReason
        data object NotDiscovered : RejectionReason
    }
}
