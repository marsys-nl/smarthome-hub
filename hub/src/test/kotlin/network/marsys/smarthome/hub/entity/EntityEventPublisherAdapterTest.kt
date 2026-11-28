package network.marsys.smarthome.hub.entity

import de.infix.testBalloon.framework.core.testSuite
import dev.nmarsman.expect.api.expectThat
import dev.nmarsman.expect.api.expectThrows
import dev.nmarsman.expect.assertions.hasMessage
import dev.nmarsman.expect.assertions.isEqualTo
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import network.marsys.smarthome.domain.entity.EntityIdentifier
import network.marsys.smarthome.domain.entity.Light
import network.marsys.smarthome.domain.entity.naming
import network.marsys.smarthome.hub.feature.entity.application.ports.inbound.ProcessEntityEvent
import network.marsys.smarthome.hub.feature.entity.domain.event.EntityProvisioned
import network.marsys.smarthome.hub.feature.entity.domain.event.Event
import network.marsys.smarthome.hub.feature.integration.application.ports.outbound.EntityEventPublisher
import kotlin.time.Instant

val EntityEventPublisherAdapterTest by testSuite(
    name = "Entity event publisher adapter tests",
) {
    val event = EntityProvisioned(
        identifier = EntityIdentifier("zigbee.0x943469fffeb3c843"),
        type = Light,
        naming = Light.naming(),
        occurredAt = Instant.parse("2026-01-01T12:00:00Z"),
    )

    test(name = "Publishing an accepted event should return Accepted") {
        val adapter = EntityEventPublisherAdapter(
            processor = {
                ProcessEntityEvent.Result.Accepted
            },
        )

        expectThat(adapter.publish(event))
            .isEqualTo(EntityEventPublisher.Result.Accepted)
    }

    test(name = "Publishing an ignored event should return Ignored") {
        val adapter = EntityEventPublisherAdapter(
            processor = {
                ProcessEntityEvent.Result.Ignored
            },
        )

        expectThat(adapter.publish(event))
            .isEqualTo(EntityEventPublisher.Result.Ignored)
    }

    test(name = "Publishing a rejected event should return Rejected") {
        val adapter = EntityEventPublisherAdapter(
            processor = {
                ProcessEntityEvent.Result.Rejected(
                    reason = ProcessEntityEvent.Result.Rejected.Reason.NotProvisioned,
                )
            },
        )

        expectThat(adapter.publish(event))
            .isEqualTo(EntityEventPublisher.Result.Rejected)
    }

    test(name = "Publishing an event should forward the original event to the processor") {
        var received: Event? = null

        val adapter = EntityEventPublisherAdapter(
            processor = {
                received = it

                ProcessEntityEvent.Result.Accepted
            },
        )

        adapter.publish(event)

        expectThat(received)
            .isEqualTo(event)
    }

    test(name = "Publishing an event should suspend until processing has completed") {
        runTest {
            val started = CompletableDeferred<Unit>()
            val completed = CompletableDeferred<Unit>()

            val adapter = EntityEventPublisherAdapter(
                processor = {
                    started.complete(Unit)
                    completed.await()
                    ProcessEntityEvent.Result.Accepted
                },
            )

            val result = async {
                adapter.publish(event)
            }

            started.await()

            expectThat(result.isCompleted)
                .isEqualTo(false)

            completed.complete(Unit)

            expectThat(result.await())
                .isEqualTo(EntityEventPublisher.Result.Accepted)
        }
    }

    test(name = "Publishing an event should propagate processing failures") {
        val adapter = EntityEventPublisherAdapter(
            processor = {
                throw IllegalStateException("Processing failed")
            },
        )

        expectThrows<IllegalStateException> { adapter.publish(event) }
            .hasMessage("Processing failed")
    }
}
