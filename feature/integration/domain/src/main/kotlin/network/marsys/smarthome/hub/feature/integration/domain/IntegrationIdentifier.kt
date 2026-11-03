package network.marsys.smarthome.hub.feature.integration.domain

import network.marsys.smarthome.domain.entity.EntityIdentifier
import network.marsys.smarthome.domain.identifiers.IntegrationIdentifier

fun IntegrationIdentifier.entity(
    identifier: String,
): EntityIdentifier = EntityIdentifier(
    value = "$namespace.${identifier}",
)
