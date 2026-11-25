package network.marsys.smarthome.hub.infrastructure.mqtt.internal

import org.eclipse.paho.mqttv5.common.util.MqttTopicValidator

@JvmInline
internal value class TopicName(val value: String) {
    init {
        require('\u0000' !in value) {
            "Topic name must not contain a null character."
        }

        MqttTopicValidator.validate(
            value,
            false,
            false,
        )
    }
}
