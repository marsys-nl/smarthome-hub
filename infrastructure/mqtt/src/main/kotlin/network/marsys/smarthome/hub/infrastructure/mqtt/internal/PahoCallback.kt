package network.marsys.smarthome.hub.infrastructure.mqtt.internal

import io.github.oshai.kotlinlogging.KotlinLogging
import network.marsys.smarthome.hub.common.annotations.ExcludeFromCoverage
import network.marsys.smarthome.hub.infrastructure.mqtt.Message
import org.eclipse.paho.mqttv5.client.IMqttToken
import org.eclipse.paho.mqttv5.client.MqttCallback
import org.eclipse.paho.mqttv5.client.MqttDisconnectResponse
import org.eclipse.paho.mqttv5.common.MqttException
import org.eclipse.paho.mqttv5.common.MqttMessage
import org.eclipse.paho.mqttv5.common.packet.MqttProperties

internal class PahoCallback(
    private val onConnectionChanged: () -> Unit,
    private val onMessageReceived: (Message) -> Unit,
) : MqttCallback {
    private val logger = KotlinLogging.logger { }

    @ExcludeFromCoverage(reason = "Required callback without application-specific behavior")
    override fun authPacketArrived(reason: Int, properties: MqttProperties) = Unit

    override fun connectComplete(reconnect: Boolean, serverURI: String) {
        onConnectionChanged.invoke()
    }

    @ExcludeFromCoverage(reason = "Required callback without application-specific behavior")
    override fun deliveryComplete(token: IMqttToken) = Unit

    override fun disconnected(disconnectResponse: MqttDisconnectResponse) {
        onConnectionChanged.invoke()
    }

    override fun messageArrived(topic: String, message: MqttMessage) {
        onMessageReceived.invoke(
            Message(
                topic = topic,
                payload = message.payload.copyOf(),
            ),
        )
    }

    @ExcludeFromCoverage(reason = "Diagnostic logging only")
    override fun mqttErrorOccurred(
        exception: MqttException,
    ) {
        logger.trace(throwable = exception) { "MQTT error occurred: ${exception.message}" }
    }
}
