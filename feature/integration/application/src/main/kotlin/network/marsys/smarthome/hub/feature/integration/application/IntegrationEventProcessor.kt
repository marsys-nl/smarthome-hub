package network.marsys.smarthome.hub.feature.integration.application

import io.github.oshai.kotlinlogging.KotlinLogging
import network.marsys.smarthome.hub.feature.entity.domain.event.EntityProvisioned
import network.marsys.smarthome.hub.feature.entity.domain.event.Event
import network.marsys.smarthome.hub.feature.integration.application.ports.outbound.EventStore

private val logger = KotlinLogging.logger {}

class IntegrationEventProcessor(
    private val eventStore: EventStore,
) {
    suspend fun process(event: Event): ProcessingResult {
        val history = eventStore.load(entity = event.identifier)

        val result = when (event) {
            is EntityProvisioned -> context(with = event) {
                processEntityProvisioned(history = history)
            }

            else -> context(with = event) {
                processEntityEvent(history = history)
            }
        }

        return result
            .also { processingResult ->
                if (processingResult is ProcessingResult.Accepted) {
                    eventStore.append(event)
                } else {
                    logger.warn {
                        "Event '${event::class.simpleName}' for entity '${event.identifier}' " +
                            "was ${processingResult::class.simpleName}."
                    }
                }
            }
    }

    context(_: EntityProvisioned)
    private fun processEntityProvisioned(
        history: Collection<Event>,
    ): ProcessingResult = when (history.isEmpty()) {
        false -> ProcessingResult.Rejected(reason = RejectionReason.AlreadyProvisioned)
        true -> ProcessingResult.Accepted
    }

    context(_: Event)
    private fun processEntityEvent(
        history: Collection<Event>,
    ): ProcessingResult = when (history.isEmpty()) {
        true -> ProcessingResult.Rejected(reason = RejectionReason.NotProvisioned)
        false -> ProcessingResult.Accepted
    }

    sealed interface ProcessingResult {
        data object Accepted : ProcessingResult
        data class Rejected(
            val reason: RejectionReason,
        ) : ProcessingResult
    }

    sealed interface RejectionReason {
        data object AlreadyProvisioned : RejectionReason
        data object NotProvisioned : RejectionReason
    }
}
