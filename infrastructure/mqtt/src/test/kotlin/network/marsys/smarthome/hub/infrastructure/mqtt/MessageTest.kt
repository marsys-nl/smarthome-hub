package network.marsys.smarthome.hub.infrastructure.mqtt

import de.infix.testBalloon.framework.core.testSuite
import dev.nmarsman.expect.api.expectThat
import dev.nmarsman.expect.assertions.isEqualTo
import dev.nmarsman.expect.assertions.isNotEqualTo

val MessageTest by testSuite(
    name = "Message tests",
) {
    test(name = "Messages isn't equal to some other object") {
        val first = Message("home/light", byteArrayOf(1, 2))
        val second = "Some other object"

        expectThat(first)
            .get { equals(second) }
            .isEqualTo(false)
    }

    test(name = "Message compared to itself is equal") {
        val first = Message("home/light", byteArrayOf(1, 2))
        val second = first

        expectThat(first)
            .isEqualTo(second)
    }

    test(name = "Messages with equal payloads are equal") {
        val first = Message("home/light", byteArrayOf(1, 2))
        val second = Message("home/light", byteArrayOf(1, 2))

        expectThat(first)
            .isEqualTo(second)
    }

    test(name = "Equal messages have equal hash codes") {
        val first = Message("home/light", byteArrayOf(1, 2))
        val second = Message("home/light", byteArrayOf(1, 2))

        expectThat(first.hashCode())
            .isEqualTo(second.hashCode())
    }

    test(name = "Messages with different topics are not equal") {
        val first = Message("home/light", byteArrayOf(1, 2))
        val second = Message("home/switch", byteArrayOf(1, 2))

        expectThat(first)
            .isNotEqualTo(second)
    }

    test(name = "Messages with different payloads are not equal") {
        val first = Message("home/light", byteArrayOf(1, 2))
        val second = Message("home/light", byteArrayOf(1, 3))

        expectThat(first)
            .isNotEqualTo(second)
    }
}
