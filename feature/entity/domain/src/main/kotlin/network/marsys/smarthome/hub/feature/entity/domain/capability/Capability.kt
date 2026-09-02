package network.marsys.smarthome.hub.feature.entity.domain.capability

sealed class Capability<T> {
    abstract val current: T
    abstract val context: Context

    protected abstract fun create(current: T, context: Context): Capability<T>

    fun updateWith(value: T): Capability<T> =
        create(value, context)

    fun withContext(context: Context): Capability<T> =
        create(current, context)

    /*
     * Context
     */

    interface Context {
        operator fun <E : Element> get(key: Key<E>): E?
        operator fun plus(context: Context): Context

        operator fun contains(element: Element): Boolean =
            get(element.key) != null

        interface Key<E : Element>
        interface Element : Context {
            val key: Key<*>

            @Suppress("UNCHECKED_CAST")
            override fun <E : Element> get(key: Key<E>): E? =
                takeIf { this.key == key } as? E

            override fun plus(context: Context): Context =
                when (context) {
                    Empty -> this
                    else -> error("Unsupported addition of context '$context'")
                }
        }

        data object Empty : Context {
            override fun <E : Element> get(key: Key<E>): E? = null
            override fun plus(context: Context): Context = context
        }
    }

    infix fun with(context: Context): Capability<T> =
        withContext(this.context + context)

    /*
     * Constraints
     */

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
