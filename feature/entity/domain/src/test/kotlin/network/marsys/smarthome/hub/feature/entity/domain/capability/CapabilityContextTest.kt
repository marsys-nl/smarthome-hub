package network.marsys.smarthome.hub.feature.entity.domain.capability

import de.infix.testBalloon.framework.core.testSuite
import dev.nmarsman.expect.api.expectThat
import dev.nmarsman.expect.api.expectThrows
import dev.nmarsman.expect.assertions.hasMessage
import dev.nmarsman.expect.assertions.isA
import dev.nmarsman.expect.assertions.isFalse
import dev.nmarsman.expect.assertions.isTrue
import network.marsys.smarthome.hub.feature.entity.domain.capability.Capability.Context.Empty
import network.marsys.smarthome.hub.feature.entity.domain.entity.System

val CapabilityContextTest by testSuite(
    name = "Capbility context tests",
) {
    test(name = "Setting context for capability succeeds if there was no context set") {
        val capability = OnOff(current = true)

        expectThat(capability with System.MemoryType.Total)
            .get(Capability<*>::context)
            .isA<System.MemoryType.Total>()
    }

    test(name = "Setting context for capability fails if there was already some context set") {
        val capability = OnOff(current = true)

        expectThrows<IllegalStateException> {
            capability with System.MemoryType.Total with System.MemoryType.Available
        }.hasMessage("Unsupported addition of context 'Available'")
    }

    test(name = "Setting Empty context for capability succeeds if there was no context set") {
        val capability = OnOff(current = true)

        expectThat(capability with Empty)
            .get(Capability<*>::context)
            .isA<Empty>()
    }

    test(name = "Setting Empty context for capability keeps current set context") {
        val capability = OnOff(current = true)

        expectThat(capability with System.MemoryType.Total with Empty)
            .get(Capability<*>::context)
            .isA<System.MemoryType.Total>()
    }

    test(name = "Checking if a capability has some context without setting returns false") {
        val capability = OnOff(current = true)

        expectThat(System.MemoryType.Total in capability.context)
            .isFalse()
    }

    test(name = "Checking if a capability has some context while the context is set returns true") {
        val capability = OnOff(current = true) with System.MemoryType.Total

        expectThat(System.MemoryType.Total in capability.context)
            .isTrue()
    }

    test(name = "Checking if a capability has some context while some other context is set returns false") {
        val capability = OnOff(current = true) with System.MemoryType.Available

        expectThat(System.MemoryType.Total in capability.context)
            .isFalse()
    }
}
