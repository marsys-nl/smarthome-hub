package network.marsys.smarthome.hub.feature.entity.domain.reducer

import de.infix.testBalloon.framework.core.testSuite
import dev.nmarsman.expect.api.expectThat
import dev.nmarsman.expect.assertions.isA
import dev.nmarsman.expect.assertions.isFalse
import network.marsys.smarthome.domain.capability.Capability
import network.marsys.smarthome.domain.capability.Capability.Companion.optional
import network.marsys.smarthome.domain.capability.Capability.Companion.required
import network.marsys.smarthome.domain.capability.OnOff
import network.marsys.smarthome.domain.entity.EntityIdentifier
import network.marsys.smarthome.domain.entity.Light
import network.marsys.smarthome.domain.entity.naming
import network.marsys.smarthome.hub.feature.entity.domain.event.EntityActionAccepted
import network.marsys.smarthome.hub.feature.entity.domain.event.EntityDiscovered
import network.marsys.smarthome.hub.feature.entity.domain.event.EntityProvisioned
import kotlin.time.Clock

val EntityReducerTest by testSuite(
    name = "Entity reducer tests",
) {
    testSuite(
        name = "Reducing entity action related events",
    ) {
        val identifier = EntityIdentifier("light.living-room")

        val provisioned = EntityProvisioned(
            identifier = identifier,
            type = Light,
            naming = Light.naming(),
        )

        val discovered = EntityDiscovered(
            identifier = identifier,
            state = Light.State.Known(
                onOff = required(OnOff(current = false)),
                brightness = optional(null),
            ),
        )

        val accepted = EntityActionAccepted(
            action = OnOff.Toggle.On(
                entity = identifier,
            ),
            occurredAt = Clock.System.now(),
        )

        test(name = "Reducing entity action accepted event should not change the entity state - unknown state") {
            val provisioned = EntityProvisioned(
                identifier = identifier,
                type = Light,
                naming = Light.naming(),
            )

            expectThat(EntityReducer.reduce(listOf(provisioned, accepted)))
                .isA<Light>()
                .get(Light::state)
                .isA<Light.State.Unknown>()
        }

        test(name = "Reducing entity action accepted event should not change the entity state - known state") {
            expectThat(EntityReducer.reduce(listOf(provisioned, discovered, accepted)))
                .isA<Light>()
                .get(Light::state)
                .isA<Light.State.Known>()
                .get(Light.State.Known::onOff)
                .isA<Capability.Required<OnOff>>()
                .get(Capability.Required<OnOff>::value)
                .get(OnOff::current)
                .isFalse()
        }
    }
}
