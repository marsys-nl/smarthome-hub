package network.marsys.smarthome.hub.infrastructure.mqtt.internal

import de.infix.testBalloon.framework.core.testSuite
import dev.nmarsman.expect.api.expectThrows
import dev.nmarsman.expect.assertions.hasCause
import dev.nmarsman.expect.assertions.hasMessage
import network.marsys.smarthome.hub.infrastructure.mqtt.internal.helper.withDisconnectedClient
import org.eclipse.paho.mqttv5.common.MqttException

val PahoPublishExecutorTest by testSuite(
    name = "Paho publish executor test",
) {
    test(name = "Publishing while disconnected throws PublishException") {
        withDisconnectedClient { client ->
            val executor = PahoPublishExecutor(client)
            val topic = TopicName("test/topic")
            val payload = "Hello world".encodeToByteArray()

            expectThrows<PublishException> { executor.publish(topic, payload) }
                .hasMessage("Failed to publish message to '${topic.value}'.")
                .hasCause<MqttException>()
        }
    }
}
