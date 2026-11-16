package network.marsys.smarthome.hub.feature.entity.domain.decider

import de.infix.testBalloon.framework.core.testSuite
import dev.nmarsman.expect.api.expectThat
import dev.nmarsman.expect.assertions.isA
import network.marsys.smarthome.domain.capability.Brightness
import network.marsys.smarthome.domain.capability.Capability.Companion.optional
import network.marsys.smarthome.domain.capability.Capability.Companion.required
import network.marsys.smarthome.domain.capability.OnOff
import network.marsys.smarthome.domain.capability.TargetTemperature
import network.marsys.smarthome.domain.entity.EntityIdentifier
import network.marsys.smarthome.domain.entity.Light
import network.marsys.smarthome.domain.entity.naming
import network.marsys.smarthome.domain.unit.celsius
import network.marsys.smarthome.domain.unit.percent
import network.marsys.smarthome.hub.feature.entity.domain.EntityActionDecision

val LightEntityActionDeciderTest by testSuite(
    name = "Light entity action decider tests",
) {
    test(name = "Toggling a light entity off should return an accepted decision when state is known") {
        val action = OnOff.Toggle.Off(
            entity = EntityIdentifier("light.living-room"),
        )

        val entity = Light(
            identifier = EntityIdentifier("light.living-room"),
            naming = Light.naming(),
            state = Light.State.Known(
                onOff = required(OnOff(current = true)),
                brightness = optional(null),
            ),
        )

        expectThat(EntityActionDecider.apply(entity, action))
            .isA<EntityActionDecision.Accepted>()
    }

    test(name = "Toggling a light entity on should return an accepted decision when state is known") {
        val action = OnOff.Toggle.On(
            entity = EntityIdentifier("light.living-room"),
        )

        val entity = Light(
            identifier = EntityIdentifier("light.living-room"),
            naming = Light.naming(),
            state = Light.State.Known(
                onOff = required(OnOff(current = false)),
                brightness = optional(null),
            ),
        )

        expectThat(EntityActionDecider.apply(entity, action))
            .isA<EntityActionDecision.Accepted>()
    }

    test(name = "Toggling a light entity off should return a rejected decision when light is already off") {
        val action = OnOff.Toggle.Off(
            entity = EntityIdentifier("light.living-room"),
        )

        val entity = Light(
            identifier = EntityIdentifier("light.living-room"),
            naming = Light.naming(),
            state = Light.State.Known(
                onOff = required(OnOff(current = false)),
                brightness = optional(null),
            ),
        )

        expectThat(EntityActionDecider.apply(entity, action))
            .isA<EntityActionDecision.Rejected>()
            .get(EntityActionDecision.Rejected::reason)
            .isA<EntityActionDecision.Reason.DuplicateState>()
    }

    test(name = "Toggling a light entity on should return a rejected decision when light is already on") {
        val action = OnOff.Toggle.On(
            entity = EntityIdentifier("light.living-room"),
        )

        val entity = Light(
            identifier = EntityIdentifier("light.living-room"),
            naming = Light.naming(),
            state = Light.State.Known(
                onOff = required(OnOff(current = true)),
                brightness = optional(null),
            ),
        )

        expectThat(EntityActionDecider.apply(entity, action))
            .isA<EntityActionDecision.Rejected>()
            .get(EntityActionDecision.Rejected::reason)
            .isA<EntityActionDecision.Reason.DuplicateState>()
    }

    test(name = "Setting the brightness of a light entity should return an accepted decision when brightness is supported") {
        val action = Brightness.SetBrightness(
            entity = EntityIdentifier("light.living-room"),
            brightness = 25.percent,
        )

        val entity = Light(
            identifier = EntityIdentifier("light.living-room"),
            naming = Light.naming(),
            state = Light.State.Known(
                onOff = required(OnOff(current = false)),
                brightness = optional(Brightness(current = 50.percent)),
            ),
        )

        expectThat(EntityActionDecider.apply(entity, action))
            .isA<EntityActionDecision.Accepted>()
    }

    test(name = "Setting the brightness of a light entity should return a rejected decision when brightness is unsupported") {
        val action = Brightness.SetBrightness(
            entity = EntityIdentifier("light.living-room"),
            brightness = 25.percent,
        )

        val entity = Light(
            identifier = EntityIdentifier("light.living-room"),
            naming = Light.naming(),
            state = Light.State.Known(
                onOff = required(OnOff(current = false)),
                brightness = optional(null),
            ),
        )

        expectThat(EntityActionDecider.apply(entity, action))
            .isA<EntityActionDecision.Rejected>()
            .get(EntityActionDecision.Rejected::reason)
            .isA<EntityActionDecision.Reason.Unsupported>()
    }

    test(name = "Toggling a light entity should return an accepted decision when state is unknown") {
        val action = OnOff.Toggle.On(
            entity = EntityIdentifier("light.living-room"),
        )

        val entity = Light(
            identifier = EntityIdentifier("light.living-room"),
            naming = Light.naming(),
            state = Light.State.Unknown,
        )

        expectThat(EntityActionDecider.apply(entity, action))
            .isA<EntityActionDecision.Accepted>()
    }

    test(name = "Setting the brightness of a light entity should return an accepted decision when state is unknown") {
        val action = Brightness.SetBrightness(
            entity = EntityIdentifier("light.living-room"),
            brightness = 25.percent,
        )

        val entity = Light(
            identifier = EntityIdentifier("light.living-room"),
            naming = Light.naming(),
            state = Light.State.Unknown,
        )

        expectThat(EntityActionDecider.apply(entity, action))
            .isA<EntityActionDecision.Accepted>()
    }

    test(name = "Setting the target temperature of a light entity should return an rejected decision when state is unknown") {
        val action = TargetTemperature.SetTargetTemperature(
            entity = EntityIdentifier("light.living-room"),
            temperature = 25.celsius,
        )

        val entity = Light(
            identifier = EntityIdentifier("light.living-room"),
            naming = Light.naming(),
            state = Light.State.Unknown,
        )

        expectThat(EntityActionDecider.apply(entity, action))
            .isA<EntityActionDecision.Rejected>()
            .get(EntityActionDecision.Rejected::reason)
            .isA<EntityActionDecision.Reason.Unsupported>()
    }
}
