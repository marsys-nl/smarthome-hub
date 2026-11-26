package network.marsys.smarthome.hub.infrastructure.mqtt.internal

import de.infix.testBalloon.framework.core.testSuite
import dev.nmarsman.expect.api.expectDoesNotThrow
import dev.nmarsman.expect.assertions.isEqualTo
import dev.nmarsman.expect.assertions.isNotNull

val TopicFilterMatchingTest by testSuite(
    name = "Topic filter matching tests",
) {
    listOf(
        Case(filter = "#", topic = "home/light", expected = true),
        Case(filter = "#", topic = "\$SYS/status", expected = false),

        Case(filter = "\$SYS/#", topic = "\$SYS/status", expected = true),
        Case(filter = "+/status", topic = "\$SYS/status", expected = false),

        Case(filter = "home/light", topic = "home/light", expected = true),
        Case(filter = "home/light", topic = "home/", expected = false),
        Case(filter = "home/light", topic = "home/light/state", expected = false),
        Case(filter = "home/light", topic = "home/switch", expected = false),
        Case(filter = "home/light", topic = "HOME/switch", expected = false),

        Case(filter = "+/light", topic = "home/light", expected = true),
        Case(filter = "+/light", topic = "/light", expected = true),

        Case(filter = "home/+", topic = "home/light", expected = true),
        Case(filter = "home/+", topic = "home/", expected = true),
        Case(filter = "home/+", topic = "home", expected = false),

        Case(filter = "home/#", topic = "home/light", expected = true),
        Case(filter = "home/#", topic = "home/", expected = true),
        Case(filter = "home/#", topic = "home/light/state", expected = true),
        Case(filter = "home/#", topic = "homepage/light", expected = false),

        Case(filter = "home/+/state", topic = "home/light/state", expected = true),
        Case(filter = "home/+/state", topic = "home//state", expected = true),
        Case(filter = "home/+/state", topic = "home/state", expected = false),

        Case(filter = "home/+/+", topic = "home/light/state", expected = true),
        Case(filter = "home/+/#", topic = "home/light", expected = true),
        Case(filter = "home/+/#", topic = "home", expected = false),
    ).forEach { (filter, topic, expected) ->
        test(name = "Filter '$filter' should return $expected, when matched with topic '$topic'") {
            expectDoesNotThrow { TopicFilter(value = filter) }
                .isNotNull()
                .get { matches(topic = topic) }
                .isEqualTo(expected)
        }
    }
}

private data class Case(
    val filter: String,
    val topic: String,
    val expected: Boolean,
)
