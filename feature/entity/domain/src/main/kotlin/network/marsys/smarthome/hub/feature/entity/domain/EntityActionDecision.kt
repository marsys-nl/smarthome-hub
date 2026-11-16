package network.marsys.smarthome.hub.feature.entity.domain

sealed interface EntityActionDecision {
    data object Accepted : EntityActionDecision

    data class Rejected(
        val reason: Reason,
    ) : EntityActionDecision

    sealed interface Reason {
        data object DuplicateState : Reason
        data object MismatchedEntity : Reason
        data object Unsupported : Reason
    }
}
