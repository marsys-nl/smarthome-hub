package network.marsys.smarthome.hub.infrastructure.mqtt.internal

import de.infix.testBalloon.framework.core.testSuite
import dev.nmarsman.expect.api.expectThat
import dev.nmarsman.expect.api.expectThrows
import dev.nmarsman.expect.assertions.hasCause
import dev.nmarsman.expect.assertions.hasMessage
import dev.nmarsman.expect.assertions.isFalse
import network.marsys.smarthome.hub.infrastructure.mqtt.internal.helper.withDisconnectedClient
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions
import org.eclipse.paho.mqttv5.common.MqttException

val PahoConnectionTest by testSuite(
    name = "Paho connection test",
) {
    test(name = "Connecting a closed client throws ConnectionException") {
        withDisconnectedClient { client ->
            client.close()

            val connection = PahoConnection(
                client = client,
                options = MqttConnectionOptions(),
            )

            expectThrows<ConnectionException> { connection.connect() }
                .hasMessage("Failed to connect to MQTT broker at ${client.serverURI}.")
                .hasCause<MqttException>()

            expectThat(connection.connected.value)
                .isFalse()
        }
    }

    test(name = "Disconnecting an unconnected client throws ConnectionException") {
        withDisconnectedClient { client ->
            val connection = PahoConnection(
                client = client,
                options = MqttConnectionOptions(),
            )

            expectThrows<ConnectionException> { connection.disconnect() }
                .hasMessage("Failed to disconnect from MQTT broker at ${client.serverURI}.")
                .hasCause<MqttException>()

            expectThat(connection.connected.value).isFalse()
        }
    }
}
