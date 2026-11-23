package network.marsys.smarthome.hub.feature.integration.application.internal

import de.infix.testBalloon.framework.core.testSuite
import dev.nmarsman.expect.api.expectThat
import dev.nmarsman.expect.assertions.isA
import network.marsys.smarthome.domain.capability.OnOff
import network.marsys.smarthome.domain.entity.EntityIdentifier
import network.marsys.smarthome.hub.feature.entity.application.ports.outbound.EntityActionDispatcher
import network.marsys.smarthome.hub.feature.entity.domain.event.EntityActionAccepted
import network.marsys.smarthome.hub.feature.integration.application.ports.inbound.IntegrationActionExecutor
import kotlin.time.Clock

val IntegrationEntityActionDispatcherTest by testSuite(
    name = "Integration entity action dispatcher tests",
) {
    test(name = "Dispatching an action when no executor known should return Unclaimed") {
        val dispatcher = IntegrationEntityActionDispatcher(
            executors = emptyList(),
        )

        val event = EntityActionAccepted(
            action = OnOff.Toggle.On(
                entity = EntityIdentifier("light.living-room"),
            ),
            occurredAt = Clock.System.now(),
        )

        expectThat(dispatcher.dispatch(accepted = event))
            .isA<EntityActionDispatcher.Result.Unclaimed>()
    }

    test(name = "Dispatching an action when no executor claims it should return Unclaimed") {
        val dispatcher = IntegrationEntityActionDispatcher(
            executors = listOf(
                IntegrationActionExecutor {
                    IntegrationActionExecutor.Result.NotResponsible
                },
            ),
        )

        val event = EntityActionAccepted(
            action = OnOff.Toggle.On(
                entity = EntityIdentifier("light.living-room"),
            ),
            occurredAt = Clock.System.now(),
        )

        expectThat(dispatcher.dispatch(accepted = event))
            .isA<EntityActionDispatcher.Result.Unclaimed>()
    }

    for (count in 1..10) {
        test(name = "Dispatching an action when $count executor(s) claim it should return claimed with count $count") {
            val dispatcher = IntegrationEntityActionDispatcher(
                executors = List(count) {
                    IntegrationActionExecutor {
                        IntegrationActionExecutor.Result.Claimed
                    }
                },
            )

            val event = EntityActionAccepted(
                action = OnOff.Toggle.On(
                    entity = EntityIdentifier("light.living-room"),
                ),
                occurredAt = Clock.System.now(),
            )

            expectThat(dispatcher.dispatch(accepted = event))
                .isA<EntityActionDispatcher.Result.Claimed>()
        }
    }
}
