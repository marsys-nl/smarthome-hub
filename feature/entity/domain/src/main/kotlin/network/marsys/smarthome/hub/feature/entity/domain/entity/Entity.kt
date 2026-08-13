package network.marsys.smarthome.hub.feature.entity.domain.entity

import network.marsys.smarthome.domain.identifiers.EntityIdentifier

sealed interface Entity<S : Entity.State<C>, C : Entity.Capabilities> {
    val identifier: EntityIdentifier
    val state: S

    interface Capabilities

    sealed interface State<C : Capabilities> {
        sealed interface Known<C : Capabilities> : State<C>
        sealed interface Unknown<C : Capabilities> : State<C> {
            val lastKnown: C?
                get() = null
        }
    }

    sealed interface Type<E : Entity<*, C>, C : Capabilities> {
        fun create(identifier: EntityIdentifier): E
    }
}
