package network.marsys.smarthome.hub.feature.entity.domain.capability

import network.marsys.smarthome.domain.unit.Dimension
import network.marsys.smarthome.domain.unit.Quantity

data class MeasuredDataSize(
    override val current: Quantity<Dimension.DigitalInformation>,
    override val context: Context = Context.Empty,
) : Capability<Quantity<Dimension.DigitalInformation>>() {
    override fun create(
        current: Quantity<Dimension.DigitalInformation>,
        context: Context,
    ): MeasuredDataSize = copy(
        current = current,
        context = context,
    )
}
