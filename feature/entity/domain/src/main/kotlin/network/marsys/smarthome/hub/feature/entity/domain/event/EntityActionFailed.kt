package network.marsys.smarthome.hub.feature.entity.domain.event

import network.marsys.smarthome.domain.entity.EntityIdentifier
import kotlin.time.Instant

data class EntityActionFailed(
    val accepted: EntityActionAccepted,
    val reason: Reason,
    override val occurredAt: Instant,
) : EntityActionEvent {
    override val identifier: EntityIdentifier
        get() = accepted.action.entity

    sealed interface Reason {
        data object Unreachable : Reason
        data object Unsupported : Reason
        data object Timeout : Reason
        data object Unknown : Reason
    }
}
