package network.marsys.smarthome.hub.feature.entity.domain

internal sealed interface EntityActionValidation {
    data object Accepted : EntityActionValidation

    data class Rejected(
        val reason: EntityActionDecision.Reason,
    ) : EntityActionValidation
}
