package network.marsys.smarthome.hub.infrastructure.mqtt.internal

import de.infix.testBalloon.framework.core.testSuite
import dev.nmarsman.expect.api.expectDoesNotThrow
import dev.nmarsman.expect.api.expectThat
import dev.nmarsman.expect.api.expectThrows
import dev.nmarsman.expect.assertions.isEqualTo

val TopicNameTest by testSuite(
    name = "Topic name tests",
) {
    testSuite(
        name = "Valid topic names",
    ) {
        val topics = listOf(
            "home",
            "home/light",
            "home/light/set",
            "/home/light",
            "home/light/",
            "home//light",
            "/",
            " ",
            "home/temperature sensor",
            "home/temperatuur/°C",
            "\$SYS/broker/uptime",
            "a".repeat(65_535),
        )

        topics.forEach { topic ->
            test(name = "Accepts valid topic of '$topic'.") {
                expectDoesNotThrow { TopicName(topic) }
            }
        }
    }

    testSuite(
        name = "Invalid topic names",
    ) {
        val topics = mapOf(
            "Empty topic" to "",
            "Null character" to "home/\u0000/light",
            "Single-level wildcard" to "home/+",
            "Multi-level wildcard" to "home/#",
            "Wildcard within level" to "home/li+ght",
            "Wildcard at root" to "#",
            "Oversized topic" to "a".repeat(65_536),
        )

        topics.forEach { (description, topic) ->
            test(name = "Rejects topic '$topic': $description") {
                expectThrows<IllegalArgumentException> {
                    TopicName(topic)
                }
            }
        }
    }

    test(name = "Accepts multibyte topic within maximum byte length") {
        val topic = "é".repeat(32_767)

        expectThat(TopicName(topic).value)
            .isEqualTo(topic)
    }

    test(name = "Rejects multibyte topic exceeding maximum byte length") {
        val topic = "é".repeat(32_768)

        expectThrows<IllegalArgumentException> {
            TopicName(topic)
        }
    }
}
