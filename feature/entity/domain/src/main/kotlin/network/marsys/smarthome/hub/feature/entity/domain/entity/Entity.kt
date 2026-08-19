package network.marsys.smarthome.hub.feature.entity.domain.entity

import network.marsys.smarthome.domain.identifiers.EntityIdentifier

sealed interface Entity {
    val identifier: EntityIdentifier
    val state: State

    sealed interface State {
        sealed interface Known : State
        sealed interface Unknown : State {
            val lastKnown: Known?
                get() = null
        }
    }

    sealed interface Type<E : Entity>
}
