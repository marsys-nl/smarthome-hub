package network.marsys.smarthome.hub.feature.platform.infrastructure

import de.infix.testBalloon.framework.core.testSuite
import dev.nmarsman.expect.api.expectThat
import dev.nmarsman.expect.assertions.isA
import dev.nmarsman.expect.assertions.isEqualTo
import network.marsys.smarthome.api.models.platform.PlatformEventMessage
import network.marsys.smarthome.domain.capability.OnOff
import network.marsys.smarthome.domain.entity.EntityIdentifier
import network.marsys.smarthome.hub.feature.platform.application.PlatformEvent
import kotlin.time.Instant

val PlatformEventMapperTest by testSuite(
    name = "Platform event mapper tests",
) {
    test(name = "Map a capability updated event to a platform event message succeeds") {
        val event = PlatformEvent.CapabilityUpdated(
            identifier = EntityIdentifier(value = "light.living-room"),
            capability = OnOff(current = true),
            occurredAt = Instant.parse("2024-06-01T12:00:00Z"),
        )

        expectThat(PlatformEventMapper.invoke(source = event))
            .isA<PlatformEventMessage.CapabilityUpdated>()
            .with(PlatformEventMessage.CapabilityUpdated::identifier) { isEqualTo(event.identifier.value) }
    }
}
