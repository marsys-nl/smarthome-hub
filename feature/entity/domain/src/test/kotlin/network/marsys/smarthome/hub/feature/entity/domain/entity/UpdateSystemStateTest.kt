package network.marsys.smarthome.hub.feature.entity.domain.entity

import de.infix.testBalloon.framework.core.testSuite
import dev.nmarsman.expect.api.expectThat
import dev.nmarsman.expect.api.expectThrows
import dev.nmarsman.expect.assertions.hasMessage
import dev.nmarsman.expect.assertions.isA
import dev.nmarsman.expect.assertions.isEqualTo
import network.marsys.smarthome.domain.unit.seconds
import network.marsys.smarthome.hub.feature.entity.domain.capability.Capability
import network.marsys.smarthome.hub.feature.entity.domain.capability.Capability.Companion.required
import network.marsys.smarthome.hub.feature.entity.domain.capability.Duration
import network.marsys.smarthome.hub.feature.entity.domain.capability.OnOff
import network.marsys.smarthome.hub.feature.entity.domain.capability.context.Application
import network.marsys.smarthome.hub.feature.entity.domain.capability.context.Host

val UpdateSystemStateTest by testSuite(
    name = "Update system state tests",
) {
    test("Updating a known state with a valid required capability succeeds when context matches Application") {
        val state: Entity.State = System.State.Known(
            uptime = System.Uptime(
                application = required(Duration(current = 5.seconds)),
                host = required(Duration(current = 15.seconds)),
            ),
        )

        val update = Duration(current = 10.seconds) with Application

        expectThat(state.updateWith(update))
            .isA<System.State.Known>()
            .get(System.State.Known::uptime)
            .get(System.Uptime::application)
            .get(Capability.Present<Duration>::value)
            .get(Duration::current)
            .isEqualTo(10.seconds)
    }

    test("Updating a known state with a valid capability only updates capability with matching context Application") {
        val state: Entity.State = System.State.Known(
            uptime = System.Uptime(
                application = required(Duration(current = 5.seconds)),
                host = required(Duration(current = 15.seconds)),
            ),
        )

        val update = Duration(current = 10.seconds) with Application

        expectThat(state.updateWith(update))
            .isA<System.State.Known>()
            .get(System.State.Known::uptime)
            .get(System.Uptime::host)
            .get(Capability.Present<Duration>::value)
            .get(Duration::current)
            .isEqualTo(15.seconds)
    }

    test("Updating a known state with a valid required capability succeeds when context matches Host") {
        val state: Entity.State = System.State.Known(
            uptime = System.Uptime(
                application = required(Duration(current = 5.seconds)),
                host = required(Duration(current = 15.seconds)),
            ),
        )

        val update = Duration(current = 10.seconds) with Host

        expectThat(state.updateWith(update))
            .isA<System.State.Known>()
            .get(System.State.Known::uptime)
            .get(System.Uptime::host)
            .get(Capability.Present<Duration>::value)
            .get(Duration::current)
            .isEqualTo(10.seconds)
    }

    test("Updating a known state with a valid capability only updates capability with matching context Host") {
        val state: Entity.State = System.State.Known(
            uptime = System.Uptime(
                application = required(Duration(current = 5.seconds)),
                host = required(Duration(current = 15.seconds)),
            ),
        )

        val update = Duration(current = 10.seconds) with Host

        expectThat(state.updateWith(update))
            .isA<System.State.Known>()
            .get(System.State.Known::uptime)
            .get(System.Uptime::application)
            .get(Capability.Present<Duration>::value)
            .get(Duration::current)
            .isEqualTo(5.seconds)
    }

    test("Updating a known state with an unknown context fails") {
        val state: Entity.State = System.State.Known(
            uptime = System.Uptime(
                application = required(Duration(current = 5.seconds)),
                host = required(Duration(current = 15.seconds)),
            ),
        )

        val update = Duration(current = 10.seconds) with Unsupported

        expectThrows<IllegalStateException> {
            state.updateWith(update)
        }.hasMessage("Unsupported 'Duration' capability provided for 'System.State.Known'")
    }

    test("Updating a known state with an unknown capability fails") {
        val state: Entity.State = System.State.Known(
            uptime = System.Uptime(
                application = required(Duration(current = 5.seconds)),
                host = required(Duration(current = 15.seconds)),
            ),
        )

        val update = OnOff(current = true)

        expectThrows<IllegalStateException> {
            state.updateWith(update)
        }.hasMessage("Unsupported 'OnOff' capability provided for 'System.State.Known'")
    }
}

private data object Unsupported : Capability.Context.Element {
    override val key: Capability.Context.Key<Unsupported>
        get() = Key

    data object Key : Capability.Context.Key<Unsupported>
}
