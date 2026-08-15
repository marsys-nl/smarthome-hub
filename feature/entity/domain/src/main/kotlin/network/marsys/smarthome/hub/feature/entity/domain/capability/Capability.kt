package network.marsys.smarthome.hub.feature.entity.domain.capability

sealed interface Capability<T> {
    val current: T

    sealed interface Constraint<out C : Capability<*>>

    sealed interface Present<out C : Capability<*>> : Constraint<C> {
        val value: C
    }

    data class Required<C : Capability<*>>(
        override val value: C,
    ) : Constraint<C>, Present<C>

    sealed interface Optional<out C : Capability<*>> : Constraint<C>
    data object Unsupported : Optional<Nothing>

    data class Available<C : Capability<*>>(
        override val value: C,
    ) : Optional<C>, Present<C>

    companion object {
        fun <T : Capability<*>> required(value: T) = Required(value)
        fun <T : Capability<*>> optional(value: T?) = when (value) {
            null -> Unsupported
            else -> Available(value)
        }
    }
}
