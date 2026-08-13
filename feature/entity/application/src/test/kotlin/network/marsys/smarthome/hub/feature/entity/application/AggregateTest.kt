package network.marsys.smarthome.hub.feature.entity.application

import de.infix.testBalloon.framework.core.testSuite
import dev.nmarsman.expect.api.expectThat
import dev.nmarsman.expect.api.expectThrows
import dev.nmarsman.expect.assertions.hasMessage
import dev.nmarsman.expect.assertions.isA
import dev.nmarsman.expect.assertions.isEqualTo
import network.marsys.smarthome.domain.identifiers.EntityIdentifier
import network.marsys.smarthome.hub.feature.entity.application.event.EntityProvisioned
import network.marsys.smarthome.hub.feature.entity.domain.entity.Entity
import network.marsys.smarthome.hub.feature.entity.domain.entity.Light

val AggregateTest by testSuite(
    name = "Aggregate tests",
) {
    test(name = "Cannot create aggregate without provisioning event") {
        expectThrows<IllegalStateException> {
            EntityAggregate(emptyList())
        }.hasMessage("Entity has not been provisioned.")
    }

    test(name = "Creating aggregate fails when multiple provisioning events are provided") {
        expectThrows<IllegalStateException> {
            val identifier = EntityIdentifier("light.living-room")

            val history = listOf(
                EntityProvisioned(
                    identifier = identifier,
                    type = Light,
                ),
                EntityProvisioned(
                    identifier = identifier,
                    type = Light,
                ),
            )

            EntityAggregate(history)
        }.hasMessage("Entity has already been provisioned.")
    }

    test(name = "Creates aggregate when entity provisioned event is provided") {
        val identifier = EntityIdentifier("light.living-room")

        val history = listOf(
            EntityProvisioned(
                identifier = identifier,
                type = Light,
            ),
        )

        val aggregate = EntityAggregate(history)

        expectThat(aggregate.entity)
            .isEqualTo(Light.create(identifier))

        expectThat(aggregate.entity)
            .with(Entity<*, *>::identifier) { isEqualTo(identifier) }
            .get(Entity<*, *>::state)
            .isA<Light.State.Unknown>()
    }
}
