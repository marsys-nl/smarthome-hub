package network.marsys.smarthome.hub.feature.entity.application.internal

import de.infix.testBalloon.framework.core.testSuite
import dev.nmarsman.expect.api.expectThat
import dev.nmarsman.expect.api.expectThrows
import dev.nmarsman.expect.assertions.contains
import dev.nmarsman.expect.assertions.isA
import dev.nmarsman.expect.assertions.isEmpty
import dev.nmarsman.expect.assertions.isEqualTo
import dev.nmarsman.expect.assertions.single
import jdk.internal.vm.vector.VectorSupport.store
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
import network.marsys.smarthome.hub.feature.entity.application.ports.outbound.EntityActionDispatcher
import network.marsys.smarthome.hub.feature.entity.application.ports.outbound.EventStore
import network.marsys.smarthome.hub.feature.entity.domain.event.EntityActionAccepted
import network.marsys.smarthome.hub.feature.entity.domain.event.EntityDiscovered
import network.marsys.smarthome.hub.feature.entity.domain.event.EntityProvisioned
import network.marsys.smarthome.hub.feature.entity.domain.event.Event

val ExecuteEntityActionTest by testSuite(
    name = "Execute entity action tests",
) {
    test(name = "Action is not executed when entity does not exist") {
        val store = FakeEventStore()

        val execute = ExecuteEntityActionImpl(
            dispatcher = FakeEntityActionDispatcher(),
            store = store,
            synchronization = EntitySynchronization(),
        )

        val action = OnOff.Toggle.On(
            entity = EntityIdentifier("light.living-room"),
        )

        execute.invoke(action = action)

        expectThat(store.appended)
            .isEmpty()
    }

    test(name = "Accepted action produces an accepted event") {
        val identifier = EntityIdentifier("light.living-room")

        val store = FakeEventStore(
            initial = listOf(
                EntityProvisioned(
                    identifier = identifier,
                    type = Light,
                    naming = Light.naming(),
                ),
            ),
        )

        val execute = ExecuteEntityActionImpl(
            dispatcher = FakeEntityActionDispatcher(),
            store = store,
            synchronization = EntitySynchronization(),
        )

        val action = OnOff.Toggle.On(
            entity = identifier,
        )

        execute.invoke(action = action)

        expectThat(store.appended)
            .single()
            .isA<EntityActionAccepted>()
            .get(EntityActionAccepted::action)
            .isEqualTo(action)
    }

    test(name = "Rejected action does not produce events") {
        val identifier = EntityIdentifier("light.living-room")

        val store = FakeEventStore(
            initial = listOf(
                EntityProvisioned(
                    identifier = identifier,
                    type = Light,
                    naming = Light.naming(),
                ),
                EntityDiscovered(
                    identifier = identifier,
                    state = Light.State.Known(
                        onOff = required(OnOff(current = true)),
                        brightness = optional(Brightness(current = 50.percent)),
                    ),
                ),
            ),
        )

        val execute = ExecuteEntityActionImpl(
            dispatcher = FakeEntityActionDispatcher(),
            store = store,
            synchronization = EntitySynchronization(),
        )

        val action = TargetTemperature.SetTargetTemperature(
            entity = identifier,
            temperature = 20.celsius,
        )

        execute.invoke(action = action)

        expectThat(store.appended)
            .isEmpty()
    }

    test(name = "Duplicate pending action is not accepted twice") {
        val identifier = EntityIdentifier("light.living-room")

        val store = FakeEventStore(
            initial = listOf(
                EntityProvisioned(
                    identifier = identifier,
                    type = Light,
                    naming = Light.naming(),
                ),
            ),
        )

        val execute = ExecuteEntityActionImpl(
            dispatcher = FakeEntityActionDispatcher(),
            store = store,
            synchronization = EntitySynchronization(),
        )

        val action = OnOff.Toggle.On(
            entity = identifier,
        )

        execute.invoke(action = action)
        execute.invoke(action = action)

        expectThat(store.appended)
            .single()
    }

    test(name = "Accepted action is dispatched") {
        val identifier = EntityIdentifier("light.living-room")

        val dispatcher = FakeEntityActionDispatcher()

        val store = FakeEventStore(
            initial = listOf(
                EntityProvisioned(
                    identifier = identifier,
                    type = Light,
                    naming = Light.naming(),
                ),
            ),
        )

        val execute = ExecuteEntityActionImpl(
            dispatcher = dispatcher,
            store = store,
            synchronization = EntitySynchronization(),
        )

        val action = OnOff.Toggle.On(
            entity = identifier,
        )

        execute.invoke(action = action)

        expectThat(dispatcher.dispatched)
            .single()
            .get(EntityActionAccepted::action)
            .isEqualTo(action)
    }

    test(name = "Rejected action is not dispatched") {
        val identifier = EntityIdentifier("light.living-room")

        val dispatcher = FakeEntityActionDispatcher()

        val store = FakeEventStore(
            initial = listOf(
                EntityProvisioned(
                    identifier = identifier,
                    type = Light,
                    naming = Light.naming(),
                ),
                EntityDiscovered(
                    identifier = identifier,
                    state = Light.State.Known(
                        onOff = required(OnOff(current = true)),
                        brightness = optional(Brightness(current = 50.percent)),
                    ),
                ),
            ),
        )

        val execute = ExecuteEntityActionImpl(
            dispatcher = dispatcher,
            store = store,
            synchronization = EntitySynchronization(),
        )

        val action = TargetTemperature.SetTargetTemperature(
            entity = identifier,
            temperature = 20.celsius,
        )

        execute.invoke(action = action)

        expectThat(dispatcher.dispatched)
            .isEmpty()
    }

    test(name = "Action is dispatched after saving event") {
        val identifier = EntityIdentifier("light.living-room")

        val store = FakeEventStore(
            initial = listOf(
                EntityProvisioned(
                    identifier = identifier,
                    type = Light,
                    naming = Light.naming(),
                ),
            ),
        )

        val dispatcher = EntityActionDispatcher { accepted ->
            val history = store.load(entity = accepted.action.entity)

            expectThat(history)
                .contains(accepted)
        }

        val execute = ExecuteEntityActionImpl(
            dispatcher = dispatcher,
            store = store,
            synchronization = EntitySynchronization(),
        )

        val action = OnOff.Toggle.On(
            entity = identifier,
        )

        execute.invoke(action = action)
    }

    test(name = "Action is not dispatched when saving event fails") {
        val identifier = EntityIdentifier("light.living-room")

        val store = FakeEventStore(
            initial = listOf(
                EntityProvisioned(
                    identifier = identifier,
                    type = Light,
                    naming = Light.naming(),
                ),
            ),
            appendingFailure = IllegalStateException("Some simulated failure occurred while appending events."),
        )

        val dispatcher = FakeEntityActionDispatcher()

        val execute = ExecuteEntityActionImpl(
            dispatcher = dispatcher,
            store = store,
            synchronization = EntitySynchronization(),
        )

        val action = OnOff.Toggle.On(
            entity = identifier,
        )

        expectThrows<IllegalStateException> {
            execute.invoke(action = action)
        }

        expectThat(dispatcher.dispatched)
            .isEmpty()
    }
}

private class FakeEntityActionDispatcher : EntityActionDispatcher {
    val dispatched = mutableListOf<EntityActionAccepted>()

    override suspend fun dispatch(accepted: EntityActionAccepted) {
        dispatched += accepted
    }
}

private class FakeEventStore(
    private val initial: Collection<Event> = emptyList(),
    private val appendingFailure: Throwable? = null,
) : EventStore {
    val appended = mutableListOf<Event>()

    override suspend fun append(vararg events: Event) {
        appendingFailure?.let { throw it }
        appended.addAll(elements = events.toList())
    }

    override suspend fun load(entity: EntityIdentifier): Collection<Event> =
        (initial + appended).filter { it.identifier == entity }
}
