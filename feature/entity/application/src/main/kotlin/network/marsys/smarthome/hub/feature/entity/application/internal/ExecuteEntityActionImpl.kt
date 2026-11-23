package network.marsys.smarthome.hub.feature.entity.application.internal

import network.marsys.smarthome.domain.entity.Entity
import network.marsys.smarthome.hub.feature.entity.application.ports.inbound.ExecuteEntityAction
import network.marsys.smarthome.hub.feature.entity.application.ports.inbound.ProcessEntityEvent
import network.marsys.smarthome.hub.feature.entity.application.ports.outbound.EntityActionDispatcher
import network.marsys.smarthome.hub.feature.entity.application.ports.outbound.EventStore
import network.marsys.smarthome.hub.feature.entity.domain.EntityActionDecision
import network.marsys.smarthome.hub.feature.entity.domain.EntityAggregate
import network.marsys.smarthome.hub.feature.entity.domain.event.EntityActionAccepted
import network.marsys.smarthome.hub.feature.entity.domain.event.EntityActionFailed
import kotlin.time.Clock

internal class ExecuteEntityActionImpl(
    private val dispatcher: EntityActionDispatcher,
    private val processor: ProcessEntityEvent,
    private val store: EventStore,
    private val synchronization: EntitySynchronization,
) : ExecuteEntityAction {
    override suspend fun invoke(action: Entity.Action): EntityActionDecision =
        synchronization.withLock(entity = action.entity) {
            val history = store.load(entity = action.entity)

            if (history.isEmpty()) {
                return@withLock EntityActionDecision.Rejected(
                    reason = EntityActionDecision.Reason.MismatchedEntity,
                )
            }

            EntityAggregate(history = history)
                .apply(action = action)
                .also { result ->
                    if (result is EntityActionDecision.Accepted) {
                        store.append(*result.events.toTypedArray())
                    }
                }
        }.also { decision ->
            if (decision is EntityActionDecision.Accepted) {
                dispatch(decision = decision)
            }
        }

    private suspend fun dispatch(decision: EntityActionDecision.Accepted) {
        val event = decision.events
            .filterIsInstance<EntityActionAccepted>()
            .single()

        val result = dispatcher.dispatch(accepted = event)

        when (result) {
            is EntityActionDispatcher.Result.Unclaimed ->
                processor.invoke(
                    event = EntityActionFailed(
                        accepted = event,
                        occurredAt = Clock.System.now(),
                    ),
                )

            is EntityActionDispatcher.Result.Claimed -> Unit
        }
    }
}
