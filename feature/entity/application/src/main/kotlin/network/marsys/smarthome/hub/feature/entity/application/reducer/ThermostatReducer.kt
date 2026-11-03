package network.marsys.smarthome.hub.feature.entity.application.reducer

import network.marsys.smarthome.domain.entity.EntityIdentifier
import network.marsys.smarthome.domain.entity.EntityNaming
import network.marsys.smarthome.domain.entity.Thermostat
import network.marsys.smarthome.hub.feature.entity.domain.event.Event

internal object ThermostatReducer : EntityReducer<Thermostat, Thermostat.State, Thermostat.State.Known, Thermostat.State.Unknown>() {
    override fun createUnknownState() =
        Thermostat.State.Unknown

    override fun reduce(
        identifier: EntityIdentifier,
        naming: EntityNaming,
        history: Collection<Event>,
    ): Thermostat = history
        .fold(
            initial = Thermostat(
                identifier = identifier,
                naming = naming,
                state = createUnknownState(),
            ),
            operation = { entity, event ->
                reduce<Thermostat.State.Known>(entity = entity, event = event)
            },
        )

    override fun Thermostat.update(state: Thermostat.State): Thermostat =
        copy(state = state)
}
