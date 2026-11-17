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
import network.marsys.smarthome.domain.entity.Speaker
import network.marsys.smarthome.domain.entity.naming
import network.marsys.smarthome.hub.feature.entity.domain.event.EntityBecameUnavailable
import network.marsys.smarthome.hub.feature.entity.domain.event.EntityDiscovered
import network.marsys.smarthome.hub.feature.entity.domain.event.EntityProvisioned

val SpeakerReducerTest by testSuite(
    name = "Speaker reducer tests",
) {
    val identifier = EntityIdentifier("speaker.living-room")
    val naming = Speaker.naming(
        key = TranslationKey("living-room"),
    )

    test(name = "Reducing an empty history of events result a speaker with unknown state") {
        val result = SpeakerReducer.reduce(
            identifier = identifier,
            naming = naming,
            history = emptyList(),
        )

        expectThat(result)
            .get(Speaker::state)
            .isA<Speaker.State.Unknown>()
    }

    test(name = "Reducing a history of events containing a provisioned event fails") {
        expectThrows<IllegalStateException> {
            SpeakerReducer.reduce(
                identifier = identifier,
                naming = naming,
                history = listOf(
                    EntityProvisioned(
                        identifier = identifier,
                        type = Speaker,
                        naming = naming,
                    ),
                ),
            )
        }.hasMessage("Entity has already been provisioned.")
    }

    test(name = "Reducing a history of events ending with a discovered event results in a known state") {
        val result = SpeakerReducer.reduce(
            identifier = identifier,
            naming = naming,
            history = listOf(
                EntityDiscovered(
                    identifier = identifier,
                    state = Speaker.State.Known(
                        onOff = required(OnOff(current = true)),
                    ),
                ),
            ),
        )

        expectThat(result)
            .get(Speaker::state)
            .isA<Speaker.State.Known>()
    }

    test(name = "Reducing a history of events ending with an became unavailable results in an unknown state") {
        val result = SpeakerReducer.reduce(
            identifier = identifier,
            naming = naming,
            history = listOf(
                EntityDiscovered(
                    identifier = identifier,
                    state = Speaker.State.Known(
                        onOff = required(OnOff(current = true)),
                    ),
                ),
                EntityBecameUnavailable(
                    identifier = identifier,
                ),
            ),
        )

        expectThat(result)
            .get(Speaker::state)
            .isA<Speaker.State.Unknown>()
    }
}
