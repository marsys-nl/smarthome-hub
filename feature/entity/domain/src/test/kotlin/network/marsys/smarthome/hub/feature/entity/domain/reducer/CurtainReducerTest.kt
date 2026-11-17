package network.marsys.smarthome.hub.feature.entity.domain.reducer

import de.infix.testBalloon.framework.core.testSuite
import dev.nmarsman.expect.api.expectThat
import dev.nmarsman.expect.api.expectThrows
import dev.nmarsman.expect.assertions.hasMessage
import dev.nmarsman.expect.assertions.isA
import network.marsys.smarthome.domain.TranslationKey
import network.marsys.smarthome.domain.capability.Capability.Companion.required
import network.marsys.smarthome.domain.capability.Opened
import network.marsys.smarthome.domain.entity.Cover
import network.marsys.smarthome.domain.entity.Curtain
import network.marsys.smarthome.domain.entity.EntityIdentifier
import network.marsys.smarthome.domain.entity.naming
import network.marsys.smarthome.hub.feature.entity.domain.event.EntityBecameUnavailable
import network.marsys.smarthome.hub.feature.entity.domain.event.EntityDiscovered
import network.marsys.smarthome.hub.feature.entity.domain.event.EntityProvisioned

val CurtainReducerTest by testSuite(
    name = "Curtain reducer tests",
) {
    val identifier = EntityIdentifier("curtain.living-room")
    val naming = Curtain.naming(
        key = TranslationKey("living-room"),
    )

    test(name = "Reducing an empty history of events result a curtain with unknown state") {
        val result = CurtainReducer.reduce(
            identifier = identifier,
            naming = naming,
            history = emptyList(),
        )

        expectThat(result)
            .get(Curtain::state)
            .isA<Curtain.State.Unknown>()
    }

    test(name = "Reducing a history of events containing a provisioned event fails") {
        expectThrows<IllegalStateException> {
            CurtainReducer.reduce(
                identifier = identifier,
                naming = naming,
                history = listOf(
                    EntityProvisioned(
                        identifier = identifier,
                        type = Curtain,
                        naming = naming,
                    ),
                ),
            )
        }.hasMessage("Entity has already been provisioned.")
    }

    test(name = "Reducing a history of events ending with a discovered event results in a known state") {
        val result = CurtainReducer.reduce(
            identifier = identifier,
            naming = naming,
            history = listOf(
                EntityDiscovered(
                    identifier = identifier,
                    state = Curtain.State.Known(
                        control = Cover.Control.WithoutPosition(
                            opened = required(Opened(current = false)),
                        ),
                    ),
                ),
            ),
        )

        expectThat(result)
            .get(Curtain::state)
            .isA<Curtain.State.Known>()
    }

    test(name = "Reducing a history of events ending with an became unavailable results in an unknown state") {
        val result = CurtainReducer.reduce(
            identifier = identifier,
            naming = naming,
            history = listOf(
                EntityDiscovered(
                    identifier = identifier,
                    state = Curtain.State.Known(
                        control = Cover.Control.WithoutPosition(
                            opened = required(Opened(current = false)),
                        ),
                    ),
                ),
                EntityBecameUnavailable(
                    identifier = identifier,
                ),
            ),
        )

        expectThat(result)
            .get(Curtain::state)
            .isA<Curtain.State.Unknown>()
    }
}
