package network.marsys.smarthome.hub.infrastructure.mqtt.internal

import de.infix.testBalloon.framework.core.testSuite
import dev.nmarsman.expect.api.expectThat
import dev.nmarsman.expect.assertions.contains
import dev.nmarsman.expect.assertions.isEmpty
import dev.nmarsman.expect.assertions.isEqualTo
import network.marsys.smarthome.hub.infrastructure.mqtt.Message
import org.eclipse.paho.mqttv5.client.MqttDisconnectResponse
import org.eclipse.paho.mqttv5.common.MqttException
import org.eclipse.paho.mqttv5.common.MqttMessage

val PahoCallbackTest by testSuite(
    name = "Paho callback tests",
) {
    test(name = "Notifies when connection is established") {
        var changes = 0

        val callback = PahoCallback(
            onConnectionChanged = { changes++ },
            onMessageReceived = {},
        )

        callback.connectComplete(
            false,
            "tcp://localhost:1883",
        )

        expectThat(changes)
            .isEqualTo(1)
    }

    test(name = "Notifies when connection is lost") {
        var changes = 0

        val callback = PahoCallback(
            onConnectionChanged = { changes++ },
            onMessageReceived = {},
        )

        callback.disconnected(
            disconnectResponse = MqttDisconnectResponse(
                MqttException(1),
            ),
        )

        expectThat(changes)
            .isEqualTo(1)
    }

    test(name = "Converts incoming message") {
        val received = mutableListOf<Message>()

        val callback = PahoCallback(
            onConnectionChanged = {},
            onMessageReceived = {
                received.add(it)
            },
        )

        callback.messageArrived(
            "home/light",
            MqttMessage(byteArrayOf(1, 2, 3)),
        )

        expectThat(received)
            .contains(
                Message(
                    topic = "home/light",
                    payload = byteArrayOf(1, 2, 3),
                ),
            )
    }

    test(name = "Does not emit connection changes when message arrives") {
        var changes = 0

        val callback = PahoCallback(
            onConnectionChanged = { changes++ },
            onMessageReceived = {},
        )

        callback.messageArrived(
            "home/light",
            MqttMessage(byteArrayOf(1, 2, 3)),
        )

        expectThat(changes)
            .isEqualTo(0)
    }

    test(name = "Does not emit messages when connection changes") {
        val received = mutableListOf<Message>()

        val callback = PahoCallback(
            onConnectionChanged = {},
            onMessageReceived = {
                received.add(it)
            },
        )

        callback.connectComplete(
            false,
            "tcp://localhost:1883",
        )

        expectThat(received)
            .isEmpty()
    }
}
