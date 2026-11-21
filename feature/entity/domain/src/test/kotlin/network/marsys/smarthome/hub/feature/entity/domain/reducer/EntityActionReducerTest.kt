package network.marsys.smarthome.hub.feature.entity.domain.reducer

import de.infix.testBalloon.framework.core.testSuite
import dev.nmarsman.expect.api.expectThat
import dev.nmarsman.expect.assertions.isEmpty
import dev.nmarsman.expect.assertions.isEqualTo
import dev.nmarsman.expect.assertions.single
import network.marsys.smarthome.domain.TranslationKey
import network.marsys.smarthome.domain.capability.Capability.Companion.optional
import network.marsys.smarthome.domain.capability.Capability.Companion.required
import network.marsys.smarthome.domain.capability.OnOff
import network.marsys.smarthome.domain.entity.EntityIdentifier
import network.marsys.smarthome.domain.entity.Light
import network.marsys.smarthome.domain.entity.naming
import network.marsys.smarthome.hub.feature.entity.domain.EntityAggregate
import network.marsys.smarthome.hub.feature.entity.domain.action.PendingAction
import network.marsys.smarthome.hub.feature.entity.domain.event.EntityActionAccepted
import network.marsys.smarthome.hub.feature.entity.domain.event.EntityActionCompleted
import network.marsys.smarthome.hub.feature.entity.domain.event.EntityActionFailed
import network.marsys.smarthome.hub.feature.entity.domain.event.EntityDiscovered
import network.marsys.smarthome.hub.feature.entity.domain.event.EntityProvisioned
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes

val EntityActionReducerTest by testSuite(
    name = "Entity action reducer tests",
) {
    test(name = "Reducing an empty history of entity action events result in a empty list of pending actions") {
        expectThat(EntityActionReducer.reduce(history = emptyList()))
            .isEmpty()
    }

    test(name = "Reducing a history of entity action events result in pending actions") {
        val instant = Clock.System.now()

        val events = listOf(
            EntityActionAccepted(
                action = OnOff.Toggle.On(
                    entity = EntityIdentifier("light.living-room"),
                ),
                occurredAt = instant,
            ),
        )

        val expected = listOf(
            PendingAction(
                action = OnOff.Toggle.On(
                    entity = EntityIdentifier("light.living-room"),
                ),
                acceptedAt = instant,
            ),
        )

        expectThat(EntityActionReducer.reduce(history = events))
            .isEqualTo(expected)
    }

    test(name = "Reducing a history of entity action events doesn't report completed actions") {
        val instant = Clock.System.now()

        val events = listOf(
            EntityActionAccepted(
                action = OnOff.Toggle.On(
                    entity = EntityIdentifier("light.living-room"),
                ),
                occurredAt = instant.minus(1.minutes),
            ),
            EntityActionCompleted(
                accepted = EntityActionAccepted(
                    action = OnOff.Toggle.On(
                        entity = EntityIdentifier("light.living-room"),
                    ),
                    occurredAt = instant.minus(1.minutes),
                ),
                occurredAt = instant,
            ),
        )

        expectThat(EntityActionReducer.reduce(history = events))
            .isEqualTo(emptyList())
    }

    test(name = "Reducing a history of entity action events doesn't report failed actions") {
        val instant = Clock.System.now()

        val events = listOf(
            EntityActionAccepted(
                action = OnOff.Toggle.On(
                    entity = EntityIdentifier("light.living-room"),
                ),
                occurredAt = instant.minus(1.minutes),
            ),
            EntityActionFailed(
                accepted = EntityActionAccepted(
                    action = OnOff.Toggle.On(
                        entity = EntityIdentifier("light.living-room"),
                    ),
                    occurredAt = instant.minus(1.minutes),
                ),
                occurredAt = instant,
            ),
        )

        expectThat(EntityActionReducer.reduce(history = events))
            .isEqualTo(emptyList())
    }

    test(name = "Earlier completed entity action events can be readded") {
        val instant = Clock.System.now()

        val events = listOf(
            EntityActionAccepted(
                action = OnOff.Toggle.On(
                    entity = EntityIdentifier("light.living-room"),
                ),
                occurredAt = instant.minus(2.minutes),
            ),
            EntityActionCompleted(
                accepted = EntityActionAccepted(
                    action = OnOff.Toggle.On(
                        entity = EntityIdentifier("light.living-room"),
                    ),
                    occurredAt = instant.minus(2.minutes),
                ),
                occurredAt = instant.minus(1.minutes),
            ),
            EntityActionAccepted(
                action = OnOff.Toggle.On(
                    entity = EntityIdentifier("light.living-room"),
                ),
                occurredAt = instant,
            ),
        )

        val expected = listOf(
            PendingAction(
                action = OnOff.Toggle.On(
                    entity = EntityIdentifier("light.living-room"),
                ),
                acceptedAt = instant,
            ),
        )

        expectThat(EntityActionReducer.reduce(history = events))
            .isEqualTo(expected)
    }

    test(name = "Reducing a history of entity action events ignores unrelated completed an failed events") {
        val instant = Clock.System.now()

        val events = listOf(
            EntityActionAccepted(
                action = OnOff.Toggle.On(
                    entity = EntityIdentifier("light.living-room"),
                ),
                occurredAt = instant.minus(1.minutes),
            ),
            EntityActionCompleted(
                accepted = EntityActionAccepted(
                    action = OnOff.Toggle.On(
                        entity = EntityIdentifier("light.kitchen"),
                    ),
                    occurredAt = instant.minus(1.minutes),
                ),
                occurredAt = instant,
            ),
            EntityActionFailed(
                accepted = EntityActionAccepted(
                    action = OnOff.Toggle.On(
                        entity = EntityIdentifier("light.kitchen"),
                    ),
                    occurredAt = instant.minus(1.minutes),
                ),
                occurredAt = instant,
            ),
        )

        val expected = listOf(
            PendingAction(
                action = OnOff.Toggle.On(
                    entity = EntityIdentifier("light.living-room"),
                ),
                acceptedAt = instant.minus(1.minutes),
            ),
        )

        expectThat(EntityActionReducer.reduce(history = events))
            .isEqualTo(expected)
    }

    test(name = "Reducing a history of entity action events result in pending actions within an aggregate") {
        val identifier = EntityIdentifier("light.living-room")
        val instant = Clock.System.now()
        val aggregate = EntityAggregate(
            history = listOf(
                EntityProvisioned(
                    identifier = identifier,
                    type = Light,
                    naming = Light.naming(
                        key = TranslationKey("living-room"),
                    ),
                    occurredAt = instant.minus(duration = 1.minutes),
                ),
                EntityDiscovered(
                    identifier = identifier,
                    state = Light.State.Known(
                        onOff = required(OnOff(current = false)),
                        brightness = optional(null),
                    ),
                    occurredAt = instant.minus(duration = 1.minutes),
                ),
                EntityActionAccepted(
                    action = OnOff.Toggle.On(
                        entity = identifier,
                    ),
                    occurredAt = instant,
                ),
            ),
        )

        expectThat(aggregate.pending)
            .single()
            .isEqualTo(
                expected = PendingAction(
                    action = OnOff.Toggle.On(
                        entity = identifier,
                    ),
                    acceptedAt = instant,
                ),
            )
    }
}
