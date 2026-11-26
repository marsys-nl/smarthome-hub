package network.marsys.smarthome.hub.infrastructure.mqtt.internal

import de.infix.testBalloon.framework.core.testSuite
import dev.nmarsman.expect.api.expectThat
import dev.nmarsman.expect.api.expectThrows
import dev.nmarsman.expect.assertions.hasMessage
import dev.nmarsman.expect.assertions.isEqualTo
import kotlin.time.Duration.Companion.seconds

val ReconnectDelayTest by testSuite(
    name = "Reconnect delay tests",
) {
    listOf(
        0 to 1.seconds,
        1 to 2.seconds,
        2 to 4.seconds,
        3 to 8.seconds,
        4 to 16.seconds,
        5 to 32.seconds,
        6 to 60.seconds,
        7 to 60.seconds,
        Int.MAX_VALUE to 60.seconds,
    ).forEach { (attempt, expected) ->
        test(name = "Returns $expected for attempt $attempt") {
            expectThat(ReconnectDelay.calculate(attempt))
                .isEqualTo(expected)
        }
    }

    test(name = "Rejects negative attempts") {
        expectThrows<IllegalArgumentException> { ReconnectDelay.calculate(-1) }
            .hasMessage("Attempt must be greater than 0, but was -1")
    }
}
