package network.marsys.smarthome.hub.feature.entity.domain.capability

import network.marsys.smarthome.domain.unit.Dimension
import network.marsys.smarthome.domain.unit.Quantity

data class Brightness(
    override val current: Quantity<Dimension.Ratio>,
    override val context: Context = Context.Empty,
) : Capability<Quantity<Dimension.Ratio>>() {
    override fun create(
        current: Quantity<Dimension.Ratio>,
        context: Context,
    ): Brightness = copy(
        current = current,
        context = context,
    )
}
