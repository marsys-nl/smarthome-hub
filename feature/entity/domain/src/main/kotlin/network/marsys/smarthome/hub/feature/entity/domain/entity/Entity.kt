package network.marsys.smarthome.hub.feature.entity.domain.entity

import network.marsys.smarthome.domain.i18n.TranslationKey
import network.marsys.smarthome.domain.identifiers.EntityIdentifier
import network.marsys.smarthome.hub.feature.entity.domain.capability.Capability
import kotlin.reflect.KClass

sealed interface Entity {
    val identifier: EntityIdentifier
    val naming: EntityNaming
    val state: State

    sealed interface State {
        fun get(capability: Capability<*>): Capability<*>?
        fun updateWith(capability: Capability<*>): State

        sealed interface Known : State
        sealed interface Unknown : State {
            val lastKnown: Known?
                get() = null

            override fun get(capability: Capability<*>): Capability<*>? = null
            override fun updateWith(capability: Capability<*>): State = this
        }
    }

    sealed interface Type<E : Entity> {
        val defaultNamingKey: TranslationKey
    }
}

fun Entity.Type<*>.naming(
    key: TranslationKey? = null,
    qualifier: TranslationKey? = null,
): EntityNaming = EntityNaming(
    key = key ?: defaultNamingKey,
    qualifier = qualifier,
)

val KClass<*>.formattedName: String get() = this.qualifiedName!!
    .replace(java.packageName, "")
    .trimStart('.')

fun Entity.State.unsupportedCapabilityError(capability: Capability<*>): Nothing =
    error("Unsupported '${capability::class.formattedName}' capability provided for '${this::class.formattedName}'")
