package network.marsys.smarthome.hub.feature.entity.domain.entity

import network.marsys.smarthome.domain.i18n.TranslationKey
import network.marsys.smarthome.domain.identifiers.EntityIdentifier
import network.marsys.smarthome.hub.feature.entity.domain.capability.Capability
import network.marsys.smarthome.hub.feature.entity.domain.capability.MeasureDataSize
import network.marsys.smarthome.hub.feature.entity.domain.capability.MeasureLoad
import network.marsys.smarthome.hub.feature.entity.domain.capability.MeasureTemperature
import kotlin.time.Instant

data class System(
    override val identifier: EntityIdentifier,
    override val naming: EntityNaming,
    override val state: State,
) : Entity {
    sealed interface State : Entity.State {
        data class Known(
            val info: HostInfo,
            val processor: Processor,
            val memory: Memory,
            val startedAt: StartedAt,
        ) : State, Entity.State.Known {
            override fun get(capability: Capability<*>): Capability<*>? = when (capability) {
                is MeasureDataSize if MemoryType.Total in capability.context ->
                    memory.total.value

                is MeasureDataSize if MemoryType.Available in capability.context ->
                    memory.available.value

                is MeasureDataSize if MemoryType.SwapTotal in capability.context ->
                    memory.swap.total.value

                is MeasureDataSize if MemoryType.SwapUsed in capability.context ->
                    memory.swap.used.value

                is MeasureLoad ->
                    processor.load.value

                is MeasureTemperature if processor.temperature is Capability.Available<*> ->
                    processor.temperature.value

                else -> null
            }

            override fun updateWith(
                capability: Capability<*>,
            ): Entity.State = when (capability) {
                is MeasureDataSize if MemoryType.Total in capability.context ->
                    copy(memory = memory.copy(total = memory.total.updateWith(capability)))

                is MeasureDataSize if MemoryType.Available in capability.context ->
                    copy(memory = memory.copy(available = memory.available.updateWith(capability)))

                is MeasureDataSize if MemoryType.SwapTotal in capability.context ->
                    copy(memory = memory.copy(swap = memory.swap.copy(total = memory.swap.total.updateWith(capability))))

                is MeasureDataSize if MemoryType.SwapUsed in capability.context ->
                    copy(memory = memory.copy(swap = memory.swap.copy(used = memory.swap.used.updateWith(capability))))

                is MeasureLoad ->
                    copy(processor = processor.copy(load = processor.load.updateWith(capability)))

                is MeasureTemperature ->
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
            val version: String?,
            val bitness: Int,
        )
    }

    data class StartedAt(
        val host: Instant,
        val application: Instant,
    )

    data class Processor(
        val load: Capability.Required<MeasureLoad>,
        val temperature: Capability.Optional<MeasureTemperature> = Capability.Unsupported,
    )

    data class Memory(
        val total: Capability.Required<MeasureDataSize>,
        val available: Capability.Required<MeasureDataSize>,
        val swap: Swap,
    ) {
        data class Swap(
            val total: Capability.Required<MeasureDataSize>,
            val used: Capability.Required<MeasureDataSize>,
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

    companion object : Entity.Type<System> {
        override val defaultNamingKey: TranslationKey = TranslationKey("system.smarthome-hub")
    }
}
