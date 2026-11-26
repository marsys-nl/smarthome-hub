package network.marsys.smarthome.hub.infrastructure.mqtt.internal

import de.infix.testBalloon.framework.core.testSuite
import dev.nmarsman.expect.api.expectDoesNotThrow
import dev.nmarsman.expect.api.expectThrows
import dev.nmarsman.expect.assertions.hasMessage
import dev.nmarsman.expect.assertions.hasMessageContaining
import dev.nmarsman.expect.assertions.isEqualTo
import dev.nmarsman.expect.assertions.isNotNull

val TopicFilterValidationTest by testSuite(
    name = "Topic filter validation tests",
) {
    test(name = "When a topic filter is initialized with an empty value, the initialization fails") {
        expectThrows<IllegalArgumentException> { TopicFilter(value = "") }
            .hasMessage("Invalid topic length, should be in range[1, 65535]!")
    }

    testSuite(
        name = "When a topic filter is initialized with only a wildcard, the initialization fails ",
    ) {
        for (wildcard in listOf("#+", "++", "##")) {
            test(name = wildcard) {
                expectThrows<IllegalArgumentException> { TopicFilter(value = wildcard) }
                    .hasMessageContaining("^Invalid usage of (single|multi)-level wildcard in topic string".toRegex())
            }
        }
    }

    testSuite(
        name = "When a valid topic filter is initialized, the initialization succeeds",
    ) {
        listOf(
            "#",
            "+",
            "+/state",
            "home/light",
            "home/#",
            "home/+",
            "home/+/+/state",
            "home/+/#",
            "home//state",
            "home/light/",
            "/home/light",
            "home/living room",
            "home/éclairage",
            "home/_light",
            "\$SYS/#",
        ).forEach { filter ->
            test(name = "A topic filter of '$filter' is accepted") {
                expectDoesNotThrow { TopicFilter(value = filter) }
                    .isNotNull()
                    .get(TopicFilter::value)
                    .isEqualTo(filter)
            }
        }
    }

    testSuite(
        name = "When an invalid topic filter is initialized, the initialization fails",
    ) {
        listOf(
            "",
            "home/#/state",
            "home/light#",
            "home/light+",
            "home/+light",
            "home/++",
            "home/#/#",
            "home/\u0000light",
            "\$share/group/home/#",
        ).forEach { filter ->
            test(name = "A topic filter of '$filter' is not accepted") {
                expectThrows<IllegalArgumentException> { TopicFilter(value = filter) }
            }
        }
    }
}
