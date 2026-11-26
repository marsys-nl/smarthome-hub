package network.marsys.smarthome.hub.infrastructure.mqtt

@Suppress("ArrayInDataClass")
data class Message(
    val topic: String,
    val payload: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Message) return false

        return topic == other.topic &&
            payload.contentEquals(other.payload)
    }

    override fun hashCode(): Int {
        var result = topic.hashCode()
        result = 31 * result + payload.contentHashCode()
        return result
    }
}
