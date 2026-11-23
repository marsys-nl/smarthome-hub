package network.marsys.smarthome.hub.feature.integration.application.internal

import de.infix.testBalloon.framework.core.testSuite
import dev.nmarsman.expect.api.expectThat
import dev.nmarsman.expect.assertions.isA
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import network.marsys.smarthome.domain.capability.OnOff
import network.marsys.smarthome.domain.entity.EntityIdentifier
import network.marsys.smarthome.domain.identifiers.IntegrationIdentifier
import network.marsys.smarthome.hub.feature.entity.application.ports.outbound.EntityActionDispatcher
import network.marsys.smarthome.hub.feature.entity.domain.event.EntityActionAccepted
import network.marsys.smarthome.hub.feature.integration.application.ports.inbound.IntegrationActionExecutor
import network.marsys.smarthome.hub.feature.integration.domain.Integration
import kotlin.time.Clock

val IntegrationEntityActionDispatcherTest by testSuite(
    name = "Integration entity action dispatcher tests",
) {
    test(name = "Dispatching an action when no executor known should return Unclaimed") {
        val dispatcher = IntegrationEntityActionDispatcher(
            query = {
                emptyList()
            },
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
            query = {
                listOf(
                    FakeIntegration(
                        executor = {
                            IntegrationActionExecutor.Result.NotResponsible
                        },
                    ),
                )
            },
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
                query = {
                    List(count) {
                        FakeIntegration(
                            executor = {
                                IntegrationActionExecutor.Result.Claimed
                            },
                        )
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

class FakeIntegration(
    private val executor: IntegrationActionExecutor,
) : Integration, IntegrationActionExecutor by executor {
    override val identifier: IntegrationIdentifier = IntegrationIdentifier("integration.fake")
    override val status: StateFlow<Integration.Status> = MutableStateFlow(value = Integration.Status.Running)
}
