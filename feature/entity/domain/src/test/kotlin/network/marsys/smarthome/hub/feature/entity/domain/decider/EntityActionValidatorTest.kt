package network.marsys.smarthome.hub.feature.entity.domain.decider

import de.infix.testBalloon.framework.core.testSuite
import dev.nmarsman.expect.api.expectThat
import dev.nmarsman.expect.assertions.isA
import network.marsys.smarthome.domain.capability.OnOff
import network.marsys.smarthome.domain.entity.EntityIdentifier
import network.marsys.smarthome.domain.entity.Light
import network.marsys.smarthome.domain.entity.Speaker
import network.marsys.smarthome.domain.entity.naming
import network.marsys.smarthome.hub.feature.entity.domain.EntityActionDecision
import network.marsys.smarthome.hub.feature.entity.domain.EntityActionValidation

val EntityActionValidatorTest by testSuite(
    name = "Entity action decider tests",
) {
    test(
        name = "Applying an entity action to an entity with a different identifier " +
            "should return a rejected decision with reason being mismatched entity",
    ) {
        val action = OnOff.Toggle.On(
            entity = EntityIdentifier("light.kitchen"),
        )

        val entity = Light(
            identifier = EntityIdentifier("light.living-room"),
            naming = Light.naming(),
            state = Light.State.Unknown,
        )

        expectThat(EntityActionValidator.validate(entity, action))
            .isA<EntityActionValidation.Rejected>()
            .get(EntityActionValidation.Rejected::reason)
            .isA<EntityActionDecision.Reason.MismatchedEntity>()
    }

    test(
        name = "Applying an entity action to an unsupported entity " +
            "should return a rejected decision with reason being unsupported",
    ) {
        val action = OnOff.Toggle.On(
            entity = EntityIdentifier("speaker.living-room"),
        )

        val entity = Speaker(
            identifier = EntityIdentifier("speaker.living-room"),
            naming = Speaker.naming(),
            state = Speaker.State.Unknown,
        )

        expectThat(EntityActionValidator.validate(entity, action))
            .isA<EntityActionValidation.Rejected>()
            .get(EntityActionValidation.Rejected::reason)
            .isA<EntityActionDecision.Reason.Unsupported>()
    }
}
