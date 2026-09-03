package network.marsys.smarthome.hub.feature.entity.domain.entity

import network.marsys.smarthome.domain.identifiers.EntityIdentifier
import network.marsys.smarthome.hub.feature.entity.domain.capability.Brightness
import network.marsys.smarthome.hub.feature.entity.domain.capability.Capability
import network.marsys.smarthome.hub.feature.entity.domain.capability.Duration
import network.marsys.smarthome.hub.feature.entity.domain.capability.MeasuredDataSize
import network.marsys.smarthome.hub.feature.entity.domain.capability.MeasuredLoad
import network.marsys.smarthome.hub.feature.entity.domain.capability.MeasuredTemperature
import network.marsys.smarthome.hub.feature.entity.domain.capability.OnOff
import network.marsys.smarthome.hub.feature.entity.domain.capability.context.Application
import network.marsys.smarthome.hub.feature.entity.domain.capability.context.Host

data class System(
    override val identifier: EntityIdentifier,
    override val state: State,
) : Entity {
    sealed interface State : Entity.State {
        data class Known(
            val info: HostInfo,
            val processor: Processor,
            val memory: Memory,
            val uptime: Uptime,
        ) : State, Entity.State.Known {
            override fun updateWith(
                capability: Capability<*>,
            ): Entity.State = when (capability) {
                is Duration if Application in capability.context ->
                    copy(uptime = uptime.copy(application = uptime.application.updateWith(capability)))

                is Duration if Host in capability.context ->
                    copy(uptime = uptime.copy(host = uptime.host.updateWith(capability)))

                is MeasuredDataSize if MemoryType.Total in capability.context ->
                    copy(memory = memory.copy(total = memory.total.updateWith(capability)))

                is MeasuredDataSize if MemoryType.Available in capability.context ->
                    copy(memory = memory.copy(available = memory.available.updateWith(capability)))

                is MeasuredDataSize if MemoryType.SwapTotal in capability.context ->
                    copy(memory = memory.copy(swap = memory.swap.copy(total = memory.swap.total.updateWith(capability))))

                is MeasuredDataSize if MemoryType.SwapUsed in capability.context ->
                    copy(memory = memory.copy(swap = memory.swap.copy(used = memory.swap.used.updateWith(capability))))

                is MeasuredLoad ->
                    copy(processor = processor.copy(load = processor.load.updateWith(capability)))

                is MeasuredTemperature ->
                    copy(processor = processor.copy(temperature = processor.temperature.updateWith(capability)))

                else -> unsupportedCapabilityError(capability = capability)
            }
        }

        data class Unknown(
            override val lastKnown: Known? = null,
        ) : State, Entity.State.Unknown
    }

    data class HostInfo(
        val device: Device,
        val operatingSystem: OperatingSystem,
    ) {
        data class Device(
            val manufacturer: String,
            val model: String,
            val architecture: String,
            val physicalCores: Int,
            val logicalCores: Int,
        )

        data class OperatingSystem(
            val description: String,
            val family: String,
            val version: String,
            val bitness: Int,
        )
    }

    data class Uptime(
        val host: Capability.Required<Duration>,
        val application: Capability.Required<Duration>,
    )

    data class Processor(
        val load: Capability.Required<MeasuredLoad>,
        val temperature: Capability.Optional<MeasuredTemperature>,
    )

    data class Memory(
        val total: Capability.Required<MeasuredDataSize>,
        val available: Capability.Required<MeasuredDataSize>,
        val swap: Swap,
    ) {
        data class Swap(
            val total: Capability.Required<MeasuredDataSize>,
            val used: Capability.Required<MeasuredDataSize>,
        )
    }

    sealed interface MemoryType {
        data object Total : MemoryType, Capability.Context.Element {
            override val key: Capability.Context.Key<Total>
                get() = Key

            data object Key : Capability.Context.Key<Total>
        }

        data object Available : MemoryType, Capability.Context.Element {
            override val key: Capability.Context.Key<Available>
                get() = Key

            data object Key : Capability.Context.Key<Available>
        }

        data object SwapTotal : MemoryType, Capability.Context.Element {
            override val key: Capability.Context.Key<SwapTotal>
                get() = Key

            data object Key : Capability.Context.Key<SwapTotal>
        }

        data object SwapUsed : MemoryType, Capability.Context.Element {
            override val key: Capability.Context.Key<SwapUsed>
                get() = Key

            data object Key : Capability.Context.Key<SwapUsed>
        }
    }

    companion object : Entity.Type<System>
}
