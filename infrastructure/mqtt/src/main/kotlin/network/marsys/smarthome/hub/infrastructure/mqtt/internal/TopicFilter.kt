package network.marsys.smarthome.hub.infrastructure.mqtt.internal

import org.eclipse.paho.mqttv5.common.util.MqttTopicValidator

@JvmInline
internal value class TopicFilter(val value: String) {
    init {
        require('\u0000' !in value) {
            "A topic filter can't contain the null character. Given topic filter: $value"
        }

        require(!value.startsWith("\$share/")) {
            "Shared subscriptions are not supported."
        }

        MqttTopicValidator.validate(value, true, false)
    }

    fun matches(topic: String): Boolean {
        if (topic.startsWith('$') && !value.startsWith('$')) {
            return false
        }

        val filterSegments = value.split(SEPARATOR)
        val topicSegments = topic.split(SEPARATOR)

        for ((index, segment) in filterSegments.withIndex()) {
            if (segment == MULTI_SEGMENT_WILDCARD) {
                return true
            }

            if (index >= topicSegments.size) {
                return false
            }

            if (segment != SINGLE_SEGMENT_WILDCARD && segment != topicSegments[index]) {
                return false
            }
        }

        return filterSegments.size == topicSegments.size
    }

    companion object {
        private const val SEPARATOR = "/"
        private const val MULTI_SEGMENT_WILDCARD = "#"
        private const val SINGLE_SEGMENT_WILDCARD = "+"
    }
}
