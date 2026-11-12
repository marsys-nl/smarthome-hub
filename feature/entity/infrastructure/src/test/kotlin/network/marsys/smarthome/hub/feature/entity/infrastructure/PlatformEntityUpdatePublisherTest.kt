package network.marsys.smarthome.hub.feature.entity.infrastructure

import de.infix.testBalloon.framework.core.testSuite
import dev.nmarsman.expect.api.expectThat
import dev.nmarsman.expect.api.expectThrows
import dev.nmarsman.expect.assertions.hasMessage
import dev.nmarsman.expect.assertions.isA
import dev.nmarsman.expect.assertions.isEmpty
import dev.nmarsman.expect.assertions.isEqualTo
import dev.nmarsman.expect.assertions.isTrue
import dev.nmarsman.expect.assertions.single
import network.marsys.smarthome.domain.capability.Capability
import network.marsys.smarthome.domain.capability.Capability.Companion.optional
import network.marsys.smarthome.domain.capability.Capability.Companion.required
import network.marsys.smarthome.domain.capability.MeasureTemperature
import network.marsys.smarthome.domain.capability.OnOff
import network.marsys.smarthome.domain.entity.EntityIdentifier
import network.marsys.smarthome.domain.entity.Light
import network.marsys.smarthome.domain.entity.System
import network.marsys.smarthome.domain.entity.naming
import network.marsys.smarthome.domain.platform.PlatformEvent
import network.marsys.smarthome.domain.unit.celsius
import network.marsys.smarthome.hub.feature.entity.domain.event.CapabilityUpdated
import network.marsys.smarthome.hub.feature.entity.domain.event.EntityBecameUnavailable
import network.marsys.smarthome.hub.feature.platform.application.PlatformEventPublisher

val PlatformEntityUpdatePublisherTest by testSuite(
    name = "Platform entity update publisher tests",
) {
    val entity = Light(
        identifier = EntityIdentifier("light"),
        naming = Light.naming(),
        state = Light.State.Known(
            onOff = required(OnOff(current = true)),
            brightness = optional(null),
        ),
    )

    test(name = "Capability update publishes a platform event") {
        val publisher = RecordingPlatformEventPublisher()
        val subject = PlatformEntityUpdatePublisher(publisher)

        val event = CapabilityUpdated(
            identifier = entity.identifier,
            capability = OnOff(current = true),
        )

        subject.publish(event, entity)

        expectThat(publisher.events)
            .single()
            .isA<PlatformEvent.CapabilityUpdated>()
    }

    test(name = "Published capability comes from updated entity") {
        val publisher = RecordingPlatformEventPublisher()
        val subject = PlatformEntityUpdatePublisher(publisher)

        val event = CapabilityUpdated(
            identifier = entity.identifier,
            capability = OnOff(current = false),
        )

        subject.publish(event, entity)

        expectThat(publisher.events)
            .single()
            .isA<PlatformEvent.CapabilityUpdated>()
            .get(PlatformEvent.CapabilityUpdated::capability)
            .isA<OnOff>()
            .get(OnOff::current)
            .isTrue()
    }

    test(name = "Capability context is preserved") {
        val publisher = RecordingPlatformEventPublisher()
        val subject = PlatformEntityUpdatePublisher(publisher)

        val entity = entity.copy(
            state = Light.State.Known(
                onOff = required(OnOff(current = true) with System.MemoryType.Available),
                brightness = optional(null),
            ),
        )

        val event = CapabilityUpdated(
            identifier = entity.identifier,
            capability = OnOff(current = true) with System.MemoryType.Available,
        )

        subject.publish(event, entity)

        expectThat(publisher.events)
            .single()
            .isA<PlatformEvent.CapabilityUpdated>()
            .get(PlatformEvent.CapabilityUpdated::capability)
            .get(Capability<*, *>::context)
            .isEqualTo(event.capability.context)
    }

    test(name = "Other domain events are not published") {
        val publisher = RecordingPlatformEventPublisher()
        val subject = PlatformEntityUpdatePublisher(publisher)

        val event = EntityBecameUnavailable(
            identifier = entity.identifier,
        )

        subject.publish(event, entity)

        expectThat(publisher.events)
            .isEmpty()
    }

    test(name = "Entity with unknown state fails explicitly") {
        val publisher = RecordingPlatformEventPublisher()
        val subject = PlatformEntityUpdatePublisher(publisher)

        val entity = entity.copy(state = Light.State.Unknown)
        val event = CapabilityUpdated(
            identifier = entity.identifier,
            capability = OnOff(current = true),
        )

        expectThrows<IllegalStateException> { subject.publish(event, entity) }
            .hasMessage("Entity state is not known for entity 'light'")
    }

    test(name = "Missing capability fails explicitly") {
        val publisher = RecordingPlatformEventPublisher()
        val subject = PlatformEntityUpdatePublisher(publisher)

        val event = CapabilityUpdated(
            identifier = entity.identifier,
            capability = MeasureTemperature(current = 25.celsius),
        )

        expectThrows<IllegalStateException> { subject.publish(event, entity) }
            .hasMessage("Updated capability 'MeasureTemperature' not found")
    }

    test(name = "Publisher failure is propagated") {
        val exception = IllegalStateException("Publishing failed")
        val publisher = PlatformEventPublisher {
            throw exception
        }

        val subject = PlatformEntityUpdatePublisher(publisher)

        val event = CapabilityUpdated(
            identifier = entity.identifier,
            capability = OnOff(current = true) with System.MemoryType.Available,
        )

        expectThrows<IllegalStateException> { subject.publish(event, entity) }
            .isEqualTo(exception)
    }
}
