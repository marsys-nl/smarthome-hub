package network.marsys.smarthome.hub.feature.entity.application.reducer

import network.marsys.smarthome.domain.identifiers.EntityIdentifier
import network.marsys.smarthome.hub.feature.entity.domain.entity.Entity
import network.marsys.smarthome.hub.feature.entity.domain.entity.Light
import network.marsys.smarthome.hub.feature.entity.domain.event.EntityProvisioned
import network.marsys.smarthome.hub.feature.entity.domain.event.Event

internal interface EntityReducer<E : Entity<S, C>, S : Entity.State<C>, C : Entity.Capabilities> {
    fun createWithUnknownState(
        identifier: EntityIdentifier,
    ): E

    fun reduce(entity: E, event: Event): E

    fun throwUnsupportedEventError(event: Event): Nothing =
        error("Unsupported event type: ${event::class.simpleName}")

    companion object {
        fun reduce(
            history: Collection<Event>,
        ): Entity<*, *> {
            val provisioned = checkNotNull(history.firstOrNull() as? EntityProvisioned) {
                "Entity event history must start with entity provisioned event."
            }

            val reducer = when (provisioned.type) {
                Light -> LightReducer
            }

            return history.drop(1)
                .fold(
                    initial = reducer.createWithUnknownState(identifier = provisioned.identifier),
                ) { entity, event ->
                    reducer.reduce(entity = entity, event = event)
                }
        }
    }
}
