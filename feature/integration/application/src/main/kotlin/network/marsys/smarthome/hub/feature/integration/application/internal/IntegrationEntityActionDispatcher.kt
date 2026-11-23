package network.marsys.smarthome.hub.feature.integration.application.internal

import io.github.oshai.kotlinlogging.KotlinLogging
import network.marsys.smarthome.hub.feature.entity.application.ports.outbound.EntityActionDispatcher
import network.marsys.smarthome.hub.feature.entity.domain.event.EntityActionAccepted
import network.marsys.smarthome.hub.feature.integration.application.ports.inbound.IntegrationActionExecutor

private val logger = KotlinLogging.logger {}

internal class IntegrationEntityActionDispatcher(
    private val executors: List<IntegrationActionExecutor>,
) : EntityActionDispatcher {
    override suspend fun dispatch(accepted: EntityActionAccepted): EntityActionDispatcher.Result {
        val claimed = executors.map { executor -> executor.execute(accepted) }
            .count { it is IntegrationActionExecutor.Result.Claimed }

        if (claimed > 1) {
            logger.warn {
                "Multiple ($claimed) integrations claimed the same action for entity '${accepted.identifier}'." +
                    "\n\t\t Action: ${accepted.action}"
            }
        }

        return when {
            claimed == 0 ->
                EntityActionDispatcher.Result.Unclaimed

            else ->
                EntityActionDispatcher.Result.Claimed(count = claimed)
        }
    }
}
