package network.marsys.smarthome.hub.feature.entity.domain.capability

import network.marsys.smarthome.domain.unit.Dimension
import network.marsys.smarthome.domain.unit.Quantity

data class Duration(
    override val current: Quantity<Dimension.Time>,
    override val context: Context = Context.Empty,
) : Capability<Quantity<Dimension.Time>>() {
    override fun create(
        current: Quantity<Dimension.Time>,
        context: Context,
    ): Duration = copy(
        current = current,
        context = context,
    )
}
