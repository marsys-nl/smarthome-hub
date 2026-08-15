package network.marsys.smarthome.hub.feature.entity.domain.capability

import de.infix.testBalloon.framework.core.testSuite
import dev.nmarsman.expect.api.expectThat
import dev.nmarsman.expect.assertions.isA
import network.marsys.smarthome.hub.feature.entity.domain.capability.Capability.Companion.optional

val CapabilityTest by testSuite(
    name = "Capability tests",
) {
    test(name = "Giving null to optional helper function results in Unsupported capability") {
        expectThat(optional(null))
            .isA<Capability.Unsupported>()
    }

    test(name = "Giving a valid capability to optional helper function results in Available capability") {
        expectThat(optional(OnOff(current = true)))
            .isA<Capability.Available<*>>()
    }
}
