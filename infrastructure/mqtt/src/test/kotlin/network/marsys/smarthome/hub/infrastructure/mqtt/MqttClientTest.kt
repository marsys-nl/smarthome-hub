package network.marsys.smarthome.hub.infrastructure.mqtt

import de.infix.testBalloon.framework.core.testSuite
import dev.nmarsman.expect.api.expectThat
import dev.nmarsman.expect.assertions.isEqualTo

val MqttClientTest by testSuite(
    name = "Mqtt client test",
) {
    test(name = "Client can be created without authentication") {
        val client = MqttClient(
            configuration = MqttClientConfiguration(
                broker = "tcp://localhost:1883",
                client = "test-client",
                username = null,
                password = null,
            ),
        )

        try {
            expectThat(client.state.value)
                .isEqualTo(ConnectionState.Disconnected)
        } finally {
            client.close()
        }
    }

    test(name = "Client can be created with authentication") {
        val client = MqttClient(
            configuration = MqttClientConfiguration(
                broker = "tcp://localhost:1883",
                client = "test-client",
                username = "test-user",
                password = "test-password",
            ),
        )

        try {
            expectThat(client.state.value)
                .isEqualTo(ConnectionState.Disconnected)
        } finally {
            client.close()
        }
    }
}
