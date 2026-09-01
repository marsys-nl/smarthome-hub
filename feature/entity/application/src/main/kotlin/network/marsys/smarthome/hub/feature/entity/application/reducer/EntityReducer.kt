package network.marsys.smarthome.hub.feature.entity.application.reducer

import network.marsys.smarthome.domain.identifiers.EntityIdentifier
import network.marsys.smarthome.hub.feature.entity.domain.entity.Entity
import network.marsys.smarthome.hub.feature.entity.domain.entity.Light
import network.marsys.smarthome.hub.feature.entity.domain.entity.System
import network.marsys.smarthome.hub.feature.entity.domain.entity.formattedName
import network.marsys.smarthome.hub.feature.entity.domain.event.EntityBecameUnavailable
import network.marsys.smarthome.hub.feature.entity.domain.event.EntityDiscovered
import network.marsys.smarthome.hub.feature.entity.domain.event.EntityProvisioned
import network.marsys.smarthome.hub.feature.entity.domain.event.Event

internal abstract class EntityReducer<E : Entity, S : Entity.State, K, U>
    where K : Entity.State.Known, K : Entity.State, U : Entity.State.Unknown, U : Entity.State {

    abstract fun createUnknownState(lastKnown: K? = null): U

    context(_: E)
    protected inline fun <reified T : K> handle(event: EntityDiscovered): K =
        checkNotNull(event.state as? T) {
            "Entity state of type '${event.state::class.formattedName}' supplied " +
                "while '${T::class.formattedName}' is expected."
        }

    @Suppress("UNCHECKED_CAST")
    context(entity: E)
    protected fun handle(event: EntityBecameUnavailable): U =
        createUnknownState(
            lastKnown = when (val state = entity.state) {
                is Entity.State.Known -> state
                is Entity.State.Unknown -> state.lastKnown
            } as K?,
        )

    abstract fun reduce(identifier: EntityIdentifier, history: Collection<Event>): E

    @Suppress("UNCHECKED_CAST")
    inline fun <reified T : K> reduce(entity: E, event: Event): E {
        check(event !is EntityProvisioned) {
            "Entity has already been provisioned."
        }

        check(event.identifier == entity.identifier) {
            "Mismatched entity identifier '${event.identifier}' supplied while '${entity.identifier}' is expected."
        }

        return entity.update(
            state = context(with = entity) {
                when (event) {
                    is EntityDiscovered -> handle<T>(event = event)
                    is EntityBecameUnavailable -> handle(event = event)
                } as S
            },
        )
    }

    abstract fun E.update(state: S): E

    companion object {
        fun reduce(
            history: Collection<Event>,
        ): Entity {
            val provisioned = checkNotNull(history.firstOrNull() as? EntityProvisioned) {
                "Entity event history must start with entity provisioned event."
            }

            val reducer = when (provisioned.type) {
                Light -> LightReducer
                System -> SystemReducer
            }

            return reducer.reduce(
                identifier = provisioned.identifier,
                history = history.drop(1),
            )
        }
    }
}
