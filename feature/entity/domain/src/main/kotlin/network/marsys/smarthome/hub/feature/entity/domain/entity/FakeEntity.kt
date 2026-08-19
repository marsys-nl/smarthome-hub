package network.marsys.smarthome.hub.feature.entity.domain.entity

import network.marsys.smarthome.domain.identifiers.EntityIdentifier

data class FakeEntity(
    override val identifier: EntityIdentifier,
    override val state: State,
) : Entity {
    sealed interface State : Entity.State {
        class Known : State, Entity.State.Known

        data class Unknown(
            override val lastKnown: Known? = null,
        ) : State, Entity.State.Unknown
    }
}
