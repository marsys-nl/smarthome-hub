package network.marsys.smarthome.hub.feature.entity.application.ports.inbound

import network.marsys.smarthome.hub.feature.entity.domain.event.Event

fun interface ProcessEntityEvent {
    suspend operator fun invoke(event: Event): Result

    sealed interface Result {
        data object Accepted : Result
        data object Ignored : Result

        data class Rejected(
            val reason: Reason,
        ) : Result {
            sealed interface Reason {
                data object AlreadyProvisioned : Reason
                data object CapabilityNotPresent : Reason
                data object NotDiscovered : Reason
                data object NotProvisioned : Reason
            }
        }
    }
}
