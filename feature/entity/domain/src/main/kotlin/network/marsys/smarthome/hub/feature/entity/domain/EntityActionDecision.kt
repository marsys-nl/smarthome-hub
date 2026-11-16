package network.marsys.smarthome.hub.feature.entity.domain

import network.marsys.smarthome.hub.feature.entity.domain.event.Event

sealed interface EntityActionDecision {
    data class Accepted(
        val events: List<Event>,
    ) : EntityActionDecision

    data class Rejected(
        val reason: Reason,
    ) : EntityActionDecision

    sealed interface Reason {
        data object DuplicateState : Reason
        data object MismatchedEntity : Reason
        data object Unsupported : Reason
    }
}
