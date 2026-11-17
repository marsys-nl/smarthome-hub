package network.marsys.smarthome.hub.feature.entity.domain.reducer

import de.infix.testBalloon.framework.core.testSuite
import dev.nmarsman.expect.api.expectThat
import dev.nmarsman.expect.api.expectThrows
import dev.nmarsman.expect.assertions.hasMessage
import dev.nmarsman.expect.assertions.isA
import network.marsys.smarthome.domain.TranslationKey
import network.marsys.smarthome.domain.capability.Capability.Companion.required
import network.marsys.smarthome.domain.capability.MeasureTemperature
import network.marsys.smarthome.domain.capability.OnOff
import network.marsys.smarthome.domain.capability.TargetTemperature
import network.marsys.smarthome.domain.capability.ThermostatMode
import network.marsys.smarthome.domain.capability.ThermostatMode.Mode
import network.marsys.smarthome.domain.entity.EntityIdentifier
import network.marsys.smarthome.domain.entity.Thermostat
import network.marsys.smarthome.domain.entity.naming
import network.marsys.smarthome.domain.unit.celsius
import network.marsys.smarthome.hub.feature.entity.domain.event.EntityBecameUnavailable
import network.marsys.smarthome.hub.feature.entity.domain.event.EntityDiscovered
import network.marsys.smarthome.hub.feature.entity.domain.event.EntityProvisioned

val ThermostatReducerTest by testSuite(
    name = "Thermostat reducer tests",
) {
    val identifier = EntityIdentifier("thermostat.living-room")
    val naming = Thermostat.naming(
        key = TranslationKey("living-room"),
    )

    test(name = "Reducing an empty history of events result a thermostat with unknown state") {
        val result = ThermostatReducer.reduce(
            identifier = identifier,
            naming = naming,
            history = emptyList(),
        )

        expectThat(result)
            .get(Thermostat::state)
            .isA<Thermostat.State.Unknown>()
    }

    test(name = "Reducing a history of events containing a provisioned event fails") {
        expectThrows<IllegalStateException> {
            ThermostatReducer.reduce(
                identifier = identifier,
                naming = naming,
                history = listOf(
                    EntityProvisioned(
                        identifier = identifier,
                        type = Thermostat,
                        naming = naming,
                    ),
                ),
            )
        }.hasMessage("Entity has already been provisioned.")
    }

    test(name = "Reducing a history of events ending with a discovered event results in a known state") {
        val result = ThermostatReducer.reduce(
            identifier = identifier,
            naming = naming,
            history = listOf(
                EntityDiscovered(
                    identifier = identifier,
                    state = Thermostat.State.Known(
                        onOff = required(OnOff(current = true)),
                        mode = required(ThermostatMode(current = Mode.Auto)),
                        temperatures = Thermostat.Temperatures(
                            target = required(TargetTemperature(current = 21.5.celsius)),
                            current = required(MeasureTemperature(current = 21.5.celsius)),
                        ),
                    ),
                ),
            ),
        )

        expectThat(result)
            .get(Thermostat::state)
            .isA<Thermostat.State.Known>()
    }

    test(name = "Reducing a history of events ending with an became unavailable results in an unknown state") {
        val result = ThermostatReducer.reduce(
            identifier = identifier,
            naming = naming,
            history = listOf(
                EntityDiscovered(
                    identifier = identifier,
                    state = Thermostat.State.Known(
                        onOff = required(OnOff(current = true)),
                        mode = required(ThermostatMode(current = Mode.Auto)),
                        temperatures = Thermostat.Temperatures(
                            target = required(TargetTemperature(current = 21.5.celsius)),
                            current = required(MeasureTemperature(current = 21.5.celsius)),
                        ),
                    ),
                ),
                EntityBecameUnavailable(
                    identifier = identifier,
                ),
            ),
        )

        expectThat(result)
            .get(Thermostat::state)
            .isA<Thermostat.State.Unknown>()
    }
}
