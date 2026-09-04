package network.marsys.smarthome.hub.feature.entity.domain.entity

import de.infix.testBalloon.framework.core.testSuite
import dev.nmarsman.expect.api.expectThat
import dev.nmarsman.expect.api.expectThrows
import dev.nmarsman.expect.assertions.hasMessage
import dev.nmarsman.expect.assertions.isA
import dev.nmarsman.expect.assertions.isEqualTo
import dev.nmarsman.expect.assertions.isFalse
import network.marsys.smarthome.domain.unit.percent
import network.marsys.smarthome.domain.unit.seconds
import network.marsys.smarthome.hub.feature.entity.domain.capability.Brightness
import network.marsys.smarthome.hub.feature.entity.domain.capability.Capability
import network.marsys.smarthome.hub.feature.entity.domain.capability.Capability.Companion.optional
import network.marsys.smarthome.hub.feature.entity.domain.capability.Capability.Companion.required
import network.marsys.smarthome.hub.feature.entity.domain.capability.MeasuredLoad
import network.marsys.smarthome.hub.feature.entity.domain.capability.OnOff

val UpdateLightStateTest by testSuite(
    name = "Update light state tests",
) {
    test("Updating a known state with a valid required capability succeeds") {
        val state: Entity.State = Light.State.Known(
            onOff = required(OnOff(current = true)),
            brightness = optional(Brightness(current = 25.percent)),
        )

        val update = OnOff(current = false)

        expectThat(state.updateWith(update))
            .isA<Light.State.Known>()
            .get(Light.State.Known::onOff)
            .get(Capability.Present<OnOff>::value)
            .get(OnOff::current)
            .isFalse()
    }

    test("Updating a known state with a valid optional capability succeeds") {
        val state: Entity.State = Light.State.Known(
            onOff = required(OnOff(current = true)),
            brightness = optional(Brightness(current = 25.percent)),
        )

        val update = Brightness(current = 50.percent)

        expectThat(state.updateWith(update))
            .isA<Light.State.Known>()
            .get(Light.State.Known::brightness)
            .isA<Capability.Present<Brightness>>()
            .get(Capability.Present<Brightness>::value)
            .isEqualTo(update)
    }

    test("Updating a known state with a valid unsupported capability succeeds") {
        val state: Entity.State = Light.State.Known(
            onOff = required(OnOff(current = true)),
            brightness = optional(null),
        )

        val update = Brightness(current = 50.percent)

        expectThat(state.updateWith(update))
            .isA<Light.State.Known>()
            .get(Light.State.Known::brightness)
            .isA<Capability.Unsupported>()
    }

    test("Updating a known state with a valid capability only updates said capability") {
        val state = Light.State.Known(
            onOff = required(OnOff(current = true)),
            brightness = optional(Brightness(current = 25.percent)),
        )

        val update = OnOff(current = false)

        expectThat(state.updateWith(update))
            .isA<Light.State.Known>()
            .get(Light.State.Known::brightness)
            .isEqualTo(state.brightness)
    }

    test("Updating a known state with a unknown capability fails") {
        val state = Light.State.Known(
            onOff = required(OnOff(current = true)),
            brightness = optional(Brightness(current = 25.percent)),
        )

        val update = MeasuredLoad(current = 50.percent)

        expectThrows<IllegalStateException> {
            state.updateWith(update)
        }.hasMessage("Unsupported 'MeasuredLoad' capability provided for 'Light.State.Known'")
    }
}
