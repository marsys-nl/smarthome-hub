package network.marsys.smarthome.hub.feature.entity.domain

import de.infix.testBalloon.framework.core.testSuite
import dev.nmarsman.expect.api.expectThat
import dev.nmarsman.expect.assertions.isA
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

val EntityAggregateActionTest by testSuite(
    name = "Entity aggregate action tests",
) {
    val identifier = EntityIdentifier("light.living-room")

    test(name = "Applying an entity action with a matching identifier should return an accepted decision") {
        val action = OnOff.Toggle.On(entity = identifier)
        val history = listOf(
            EntityProvisioned(
                identifier = identifier,
                type = Light,
                naming = Light.naming(),
            ),
        )

        val aggregate = EntityAggregate(history = history)

        expectThat(aggregate.apply(action))
            .isA<EntityActionDecision.Accepted>()
    }

    test(name = "Applying an entity action that is also pending return a rejected decision") {
        val action = OnOff.Toggle.Off(entity = identifier)
        val history = listOf(
            EntityProvisioned(
                identifier = identifier,
                type = Light,
                naming = Light.naming(),
            ),
            EntityDiscovered(
                identifier = identifier,
                state = Light.State.Known(
                    onOff = required(OnOff(current = true)),
                    brightness = optional(null),
                ),
            ),
            EntityActionAccepted(
                action = action,
                occurredAt = Clock.System.now(),
            ),
        )

        val aggregate = EntityAggregate(history = history)

        expectThat(aggregate.apply(action))
            .isA<EntityActionDecision.Rejected>()
            .get(EntityActionDecision.Rejected::reason)
            .isA<EntityActionDecision.Reason.DuplicateAction>()
    }

    test(name = "Applying an entity action that doesn't pass validation returns a rejected decision") {
        val action = OnOff.Toggle.Off(entity = EntityIdentifier("light.kitchen"))
        val history = listOf(
            EntityProvisioned(
                identifier = identifier,
                type = Light,
                naming = Light.naming(),
            ),
            EntityDiscovered(
                identifier = identifier,
                state = Light.State.Known(
                    onOff = required(OnOff(current = true)),
                    brightness = optional(null),
                ),
            ),
        )

        val aggregate = EntityAggregate(history = history)

        expectThat(aggregate.apply(action))
            .isA<EntityActionDecision.Rejected>()
    }
}
