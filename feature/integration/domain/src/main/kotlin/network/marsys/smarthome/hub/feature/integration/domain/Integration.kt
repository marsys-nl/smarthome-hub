package network.marsys.smarthome.hub.feature.integration.domain

import network.marsys.smarthome.domain.identifiers.IntegrationIdentifier

interface Integration {
    val identifier: IntegrationIdentifier
}
