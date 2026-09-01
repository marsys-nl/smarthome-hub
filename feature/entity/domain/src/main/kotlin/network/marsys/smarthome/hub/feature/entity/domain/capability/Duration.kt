package network.marsys.smarthome.hub.feature.entity.domain.capability

import network.marsys.smarthome.domain.unit.Dimension
import network.marsys.smarthome.domain.unit.Quantity

data class Duration(
    override val current: Quantity<Dimension.Time>,
) : Capability<Quantity<Dimension.Time>>
