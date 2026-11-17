package network.marsys.smarthome.hub.feature.entity.domain.reducer

import de.infix.testBalloon.framework.core.testSuite
import dev.nmarsman.expect.api.expectThat
import dev.nmarsman.expect.api.expectThrows
import dev.nmarsman.expect.assertions.hasMessage
import dev.nmarsman.expect.assertions.isA
import network.marsys.smarthome.domain.TranslationKey
import network.marsys.smarthome.domain.capability.Capability.Companion.required
import network.marsys.smarthome.domain.capability.Position
import network.marsys.smarthome.domain.entity.Blind
import network.marsys.smarthome.domain.entity.Cover
import network.marsys.smarthome.domain.entity.EntityIdentifier
import network.marsys.smarthome.domain.entity.naming
import network.marsys.smarthome.domain.unit.percent
import network.marsys.smarthome.hub.feature.entity.domain.event.EntityBecameUnavailable
import network.marsys.smarthome.hub.feature.entity.domain.event.EntityDiscovered
import network.marsys.smarthome.hub.feature.entity.domain.event.EntityProvisioned

val BlindReducerTest by testSuite(
    name = "Blind reducer tests",
) {
    val identifier = EntityIdentifier("blind.living-room")
    val naming = Blind.naming(
        key = TranslationKey("living-room"),
    )

    test(name = "Reducing an empty history of events result a blind with unknown state") {
        val result = BlindReducer.reduce(
            identifier = identifier,
            naming = naming,
            history = emptyList(),
        )

        expectThat(result)
            .get(Blind::state)
            .isA<Blind.State.Unknown>()
    }

    test(name = "Reducing a history of events containing a provisioned event fails") {
        expectThrows<IllegalStateException> {
            BlindReducer.reduce(
                identifier = identifier,
                naming = naming,
                history = listOf(
                    EntityProvisioned(
                        identifier = identifier,
                        type = Blind,
                        naming = naming,
                    ),
                ),
            )
        }.hasMessage("Entity has already been provisioned.")
    }

    test(name = "Reducing a history of events ending with a discovered event results in a known state") {
        val result = BlindReducer.reduce(
            identifier = identifier,
            naming = naming,
            history = listOf(
                EntityDiscovered(
                    identifier = identifier,
                    state = Blind.State.Known(
                        control = Cover.Control.WithPosition(
                            position = required(Position(current = 50.percent)),
                        ),
                    ),
                ),
            ),
        )

        expectThat(result)
            .get(Blind::state)
            .isA<Blind.State.Known>()
    }

    test(name = "Reducing a history of events ending with an became unavailable results in an unknown state") {
        val result = BlindReducer.reduce(
            identifier = identifier,
            naming = naming,
            history = listOf(
                EntityDiscovered(
                    identifier = identifier,
                    state = Blind.State.Known(
                        control = Cover.Control.WithPosition(
                            position = required(Position(current = 50.percent)),
                        ),
                    ),
                ),
                EntityBecameUnavailable(
                    identifier = identifier,
                ),
            ),
        )

        expectThat(result)
            .get(Blind::state)
            .isA<Blind.State.Unknown>()
    }
}
