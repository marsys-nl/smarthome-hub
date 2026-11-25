package network.marsys.smarthome.hub.infrastructure.mqtt.internal

import de.infix.testBalloon.framework.core.testSuite
import dev.nmarsman.expect.api.expectThrows
import network.marsys.smarthome.hub.infrastructure.mqtt.internal.SubscriptionAcknowledgement.validateSubscribeAcknowledgement
import network.marsys.smarthome.hub.infrastructure.mqtt.internal.SubscriptionAcknowledgement.validateUnsubscribeAcknowledgement

val SubscriptionAcknowledgementTest by testSuite(
    name = "Subscription acknowledgement tests",
) {
    val filter = TopicFilter("home/light")

    testSuite(name = "Subscribe acknowledgement") {
        val accepted = listOf(
            0x00,
            0x01,
            0x02,
        )

        accepted.forEach { reasonCode ->
            test(name = "Accepts reason code $reasonCode") {
                validateSubscribeAcknowledgement(
                    filter = filter,
                    reasons = intArrayOf(reasonCode),
                )
            }
        }

        val rejected = listOf(
            0x80,
            0x83,
            0x87,
            0x8F,
            0x97,
            0x9E,
            0xA1,
            0xA2,
        )

        rejected.forEach { reasonCode ->
            test(name = "Rejects reason code $reasonCode with a subscription exception") {
                expectThrows<SubscriptionException> {
                    validateSubscribeAcknowledgement(
                        filter = filter,
                        reasons = intArrayOf(reasonCode),
                    )
                }
            }
        }

        test(name = "Rejects reason code ${0x11} with a session exception") {
            expectThrows<SessionException> {
                validateSubscribeAcknowledgement(
                    filter = filter,
                    reasons = intArrayOf(0x11),
                )
            }
        }

        test(name = "Rejects missing reason codes") {
            expectThrows<SessionException> {
                validateSubscribeAcknowledgement(
                    filter = filter,
                    reasons = null,
                )
            }
        }

        test(name = "Rejects empty reason codes") {
            expectThrows<SessionException> {
                validateSubscribeAcknowledgement(
                    filter = filter,
                    reasons = intArrayOf(),
                )
            }
        }

        test(name = "Rejects multiple reason codes") {
            expectThrows<SessionException> {
                validateSubscribeAcknowledgement(
                    filter = filter,
                    reasons = intArrayOf(0x00, 0x00),
                )
            }
        }
    }

    testSuite(name = "Unsubscribe acknowledgement") {
        val accepted = listOf(
            0x00,
            0x11,
        )

        accepted.forEach { reasonCode ->
            test(name = "Accepts reason code $reasonCode") {
                validateUnsubscribeAcknowledgement(
                    filter = filter,
                    reasons = intArrayOf(reasonCode),
                )
            }
        }

        val rejected = listOf(
            0x80,
            0x83,
            0x87,
            0x8F,
            0x91,
            0x01,
        )

        rejected.forEach { reasonCode ->
            test(name = "Rejects reason code $reasonCode") {
                expectThrows<SessionException> {
                    validateUnsubscribeAcknowledgement(
                        filter = filter,
                        reasons = intArrayOf(reasonCode),
                    )
                }
            }
        }

        test(name = "Rejects missing reason codes") {
            expectThrows<SessionException> {
                validateUnsubscribeAcknowledgement(
                    filter = filter,
                    reasons = null,
                )
            }
        }

        test(name = "Rejects empty reason codes") {
            expectThrows<SessionException> {
                validateUnsubscribeAcknowledgement(
                    filter = filter,
                    reasons = intArrayOf(),
                )
            }
        }

        test(name = "Rejects multiple reason codes") {
            expectThrows<SessionException> {
                validateUnsubscribeAcknowledgement(
                    filter = filter,
                    reasons = intArrayOf(0x00, 0x00),
                )
            }
        }
    }
}
