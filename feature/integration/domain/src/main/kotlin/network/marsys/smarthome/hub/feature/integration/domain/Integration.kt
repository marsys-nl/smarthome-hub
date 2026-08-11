package network.marsys.smarthome.hub.feature.integration.domain

import kotlinx.coroutines.flow.StateFlow
import network.marsys.smarthome.domain.identifiers.IntegrationIdentifier

interface Integration {
    val identifier: IntegrationIdentifier
    val status: StateFlow<Status>

    sealed interface Status {
        data object Starting : Status
        data object Running : Status
        data object Degraded : Status
        data object Stopping : Status
        data object Stopped : Status
    }
}
