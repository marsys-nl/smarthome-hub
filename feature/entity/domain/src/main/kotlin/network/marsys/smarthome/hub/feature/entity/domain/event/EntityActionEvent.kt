package network.marsys.smarthome.hub.feature.entity.domain.event

sealed interface EntityActionEvent : Event

sealed interface EntityActionFinished : EntityActionEvent {
    val accepted: EntityActionAccepted
}
