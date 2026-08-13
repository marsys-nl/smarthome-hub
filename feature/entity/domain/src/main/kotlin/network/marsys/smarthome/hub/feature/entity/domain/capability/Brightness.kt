package network.marsys.smarthome.hub.feature.entity.domain.capability

import network.marsys.smarthome.domain.unit.Dimension
import network.marsys.smarthome.domain.unit.Quantity

data class Brightness(
    override val current: Quantity<Dimension.Ratio>,
) : Capability<Quantity<Dimension.Ratio>>
