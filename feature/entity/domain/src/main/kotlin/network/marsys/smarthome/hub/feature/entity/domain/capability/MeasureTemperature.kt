package network.marsys.smarthome.hub.feature.entity.domain.capability

import network.marsys.smarthome.domain.unit.Dimension
import network.marsys.smarthome.domain.unit.Quantity

data class MeasureTemperature(
    override val current: Quantity<Dimension.Temperature>,
    override val context: Context = Context.Empty,
) : Capability<Quantity<Dimension.Temperature>>() {
    override fun create(
        current: Quantity<Dimension.Temperature>,
        context: Context,
    ): MeasureTemperature = copy(
        current = current,
        context = context,
    )
}
