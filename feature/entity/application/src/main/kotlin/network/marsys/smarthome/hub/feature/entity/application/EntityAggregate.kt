package network.marsys.smarthome.hub.feature.entity.application

import network.marsys.smarthome.hub.feature.entity.application.event.EntityProvisioned
import network.marsys.smarthome.hub.feature.entity.application.event.Event
import network.marsys.smarthome.hub.feature.entity.domain.entity.Entity

internal class EntityAggregate(
    history: Collection<Event>,
) {
    val entity: Entity<*, *> = history.fold(
        initial = null,
        operation = ::apply,
    ) ?: throw IllegalStateException("Entity has not been provisioned.")

    private fun apply(entity: Entity<*, *>?, event: Event): Entity<*, *>? =
        when (event) {
            is EntityProvisioned -> {
                check(entity == null) {
                    "Entity has already been provisioned."
                }

                event.type.create(event.identifier)
            }
        }
}
