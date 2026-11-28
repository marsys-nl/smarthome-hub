package network.marsys.smarthome.hub.entity

import de.infix.testBalloon.framework.core.testSuite
import dev.nmarsman.expect.api.expectThat
import dev.nmarsman.expect.assertions.isEqualTo
import dev.nmarsman.expect.assertions.isNull
import network.marsys.smarthome.domain.entity.EntityIdentifier
import network.marsys.smarthome.domain.entity.Light

val EntityQueryAdapterTest by testSuite(
    name = "Entity query adapter tests",
) {
    test(name = "Finding an existing entity should return the entity") {
        val identifier = EntityIdentifier("light.kitchen")
        val entity = Light(
            identifier = identifier,
        )

        val adapter = EntityQueryAdapter(
            finder = { identifier ->
                expectThat(identifier)
                    .isEqualTo(entity.identifier)

                entity
            },
        )

        expectThat(adapter.findByIdentifier(entity.identifier))
            .isEqualTo(entity)
    }

    test(name = "Finding an unknown entity should return null") {
        val identifier = EntityIdentifier("unknown")
        val adapter = EntityQueryAdapter(
            finder = { null },
        )

        expectThat(adapter.findByIdentifier(identifier))
            .isNull()
    }
}
