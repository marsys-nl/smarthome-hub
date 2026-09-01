package network.marsys.smarthome.hub.feature.entity.domain.capability

sealed interface Capability<T> {
    val current: T

    fun updateWith(value: T): Capability<T>

    sealed interface Constraint<out C : Capability<*>>

    sealed interface Present<out C : Capability<*>> : Constraint<C> {
        val value: C
    }

    data class Required<C : Capability<*>>(
        override val value: C,
    ) : Constraint<C>, Present<C> {
        fun updateWith(capability: @UnsafeVariance C): Required<C> =
            copy(value = capability)
    }

    sealed class Optional<out C : Capability<*>> : Constraint<C> {
        @Suppress("UNCHECKED_CAST")
        fun updateWith(capability: @UnsafeVariance C): Optional<C> =
            when (this) {
                is Unsupported -> this
                is Available<*> -> update(capability = capability)
            } as Optional<C>
    }

    data object Unsupported : Optional<Nothing>()

    data class Available<C : Capability<*>>(
        override val value: C,
    ) : Optional<C>(), Present<C> {
        @Suppress("UNCHECKED_CAST")
        fun update(capability: Capability<*>): Available<C> =
            copy(value = capability as C)
    }

    companion object {
        fun <T : Capability<*>> required(value: T) = Required(value)
        fun <T : Capability<*>> optional(value: T?) = when (value) {
            null -> Unsupported
            else -> Available(value)
        }
    }
}
