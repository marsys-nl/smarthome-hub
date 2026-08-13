package network.marsys.smarthome.hub.feature.entity.domain.entity

import network.marsys.smarthome.domain.identifiers.EntityIdentifier

sealed interface Entity<S : Entity.State<C>, C : Entity.CapabilityDefinition> {
    val identifier: EntityIdentifier
    val state: S

    interface CapabilityDefinition

    sealed interface State<C : CapabilityDefinition> {
        sealed interface Known<C : CapabilityDefinition> : State<C>
        sealed interface Unknown<C : CapabilityDefinition> : State<C> {
            val lastKnown: C?
                get() = null
        }
    }

    sealed interface Type<E : Entity<*, C>, C : CapabilityDefinition> {
        fun create(identifier: EntityIdentifier): E
    }
}
