package network.marsys.smarthome.hub.feature.entity.domain.reducer

import de.infix.testBalloon.framework.core.testSuite
import dev.nmarsman.expect.api.expectThat
import dev.nmarsman.expect.api.expectThrows
import dev.nmarsman.expect.assertions.hasMessage
import dev.nmarsman.expect.assertions.isA
import network.marsys.smarthome.domain.TranslationKey
import network.marsys.smarthome.domain.capability.Capability.Companion.required
import network.marsys.smarthome.domain.capability.OnOff
import network.marsys.smarthome.domain.entity.EntityIdentifier
import network.marsys.smarthome.domain.entity.Fan
import network.marsys.smarthome.domain.entity.naming
import network.marsys.smarthome.hub.feature.entity.domain.event.EntityBecameUnavailable
import network.marsys.smarthome.hub.feature.entity.domain.event.EntityDiscovered
import network.marsys.smarthome.hub.feature.entity.domain.event.EntityProvisioned

val FanReducerTest by testSuite(
    name = "Fan reducer tests",
) {
    val identifier = EntityIdentifier("fan.living-room")
    val naming = Fan.naming(
        key = TranslationKey("living-room"),
    )

    test(name = "Reducing an empty history of events result a fan with unknown state") {
        val result = FanReducer.reduce(
            identifier = identifier,
            naming = naming,
            history = emptyList(),
        )

        expectThat(result)
            .get(Fan::state)
            .isA<Fan.State.Unknown>()
    }

    test(name = "Reducing a history of events containing a provisioned event fails") {
        expectThrows<IllegalStateException> {
            FanReducer.reduce(
                identifier = identifier,
                naming = naming,
                history = listOf(
                    EntityProvisioned(
                        identifier = identifier,
                        type = Fan,
                        naming = naming,
                    ),
                ),
            )
        }.hasMessage("Entity has already been provisioned.")
    }

    test(name = "Reducing a history of events ending with a discovered event results in a known state") {
        val result = FanReducer.reduce(
            identifier = identifier,
            naming = naming,
            history = listOf(
                EntityDiscovered(
                    identifier = identifier,
                    state = Fan.State.Known(
                        onOff = required(OnOff(current = true)),
                    ),
                ),
            ),
        )

        expectThat(result)
            .get(Fan::state)
            .isA<Fan.State.Known>()
    }

    test(name = "Reducing a history of events ending with an became unavailable results in an unknown state") {
        val result = FanReducer.reduce(
            identifier = identifier,
            naming = naming,
            history = listOf(
                EntityDiscovered(
                    identifier = identifier,
                    state = Fan.State.Known(
                        onOff = required(OnOff(current = true)),
                    ),
                ),
                EntityBecameUnavailable(
                    identifier = identifier,
                ),
            ),
        )

        expectThat(result)
            .get(Fan::state)
            .isA<Fan.State.Unknown>()
    }
}
