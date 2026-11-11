package network.marsys.smarthome.hub.feature.platform.infrastructure

import network.marsys.smarthome.api.mappers.Mapper
import network.marsys.smarthome.api.models.platform.ContextElementMessage
import network.marsys.smarthome.api.models.platform.ContextMessage
import network.marsys.smarthome.domain.capability.Capability
import network.marsys.smarthome.domain.entity.System
import network.marsys.smarthome.domain.entity.Thermostat

internal object ContextMapper : Mapper<Capability.Context, ContextMessage> {
    override fun invoke(source: Capability.Context): ContextMessage =
        ContextMessage(
            elements = when (source) {
                is Capability.Context.Empty -> emptyList()

                else -> source.fold(
                    initial = emptyList(),
                    operation = { acc, element ->
                        acc + ContextElementMapper.invoke(element)
                    },
                )
            },
        )
}

private object ContextElementMapper : Mapper<Capability.Context.Element, ContextElementMessage> {
    @Suppress("ktlint:standard:blank-line-between-when-conditions")
    override fun invoke(source: Capability.Context.Element): ContextElementMessage =
        when (source) {
            is System.MemoryType.Total -> ContextElementMessage.MemoryType.Total
            is System.MemoryType.Available -> ContextElementMessage.MemoryType.Available
            is System.MemoryType.SwapTotal -> ContextElementMessage.MemoryType.SwapTotal
            is System.MemoryType.SwapAvailable -> ContextElementMessage.MemoryType.SwapAvailable

            is Thermostat.TemperatureType.Current -> ContextElementMessage.TemperatureType.Current
            is Thermostat.TemperatureType.Outdoor -> ContextElementMessage.TemperatureType.Outdoor

            else -> error("Unsupported context element type: ${source::class.simpleName}")
        }
}
