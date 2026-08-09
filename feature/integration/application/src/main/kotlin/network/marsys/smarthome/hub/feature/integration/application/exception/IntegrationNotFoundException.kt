package network.marsys.smarthome.hub.feature.integration.application.exception

import network.marsys.smarthome.domain.identifiers.IntegrationIdentifier

data class IntegrationNotFoundException(val identifier: IntegrationIdentifier) :
    RuntimeException("Integration '$identifier' was not found.")
