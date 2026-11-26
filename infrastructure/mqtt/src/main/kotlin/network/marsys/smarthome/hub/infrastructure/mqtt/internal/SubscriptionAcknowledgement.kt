package network.marsys.smarthome.hub.infrastructure.mqtt.internal

internal object SubscriptionAcknowledgement {
    fun validateSubscribeAcknowledgement(
        filter: TopicFilter,
        reasons: IntArray?,
    ) {
        when (val reason = reasons?.singleOrNull()) {
            0x00, 0x01, 0x02 ->
                Unit

            0x80, 0x83, 0x87, 0x8F, 0x97, 0x9E, 0xA1, 0xA2 ->
                throw SubscriptionException(
                    message = "Failed to subscribe to '${filter.value}'. Reason code: $reason.",
                )

            else ->
                throw SessionException(
                    message = "Invalid subscription acknowledgement for '${filter.value}'. Reason code: $reason.",
                )
        }
    }

    fun validateUnsubscribeAcknowledgement(
        filter: TopicFilter,
        reasons: IntArray?,
    ) {
        when (val reason = reasons?.singleOrNull()) {
            0x00, 0x11 ->
                Unit

            else ->
                throw SessionException(
                    message = "Failed to unsubscribe from '${filter.value}'. Reason code: $reason.",
                )
        }
    }
}
