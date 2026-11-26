package network.marsys.smarthome.hub.infrastructure.mqtt.internal.helper

import org.eclipse.paho.mqttv5.client.MqttToken

internal class FakeMqttToken(
    private val reasons: IntArray = intArrayOf(0),
) : MqttToken("test-client") {
    override fun getReasonCodes(): IntArray =
        reasons
}
