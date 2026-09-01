package network.marsys.smarthome.hub.feature.entity.domain.entity

import network.marsys.smarthome.domain.identifiers.EntityIdentifier
import network.marsys.smarthome.hub.feature.entity.domain.capability.Capability
import kotlin.reflect.KClass

sealed interface Entity {
    val identifier: EntityIdentifier
    val state: State

    sealed interface State {
        fun updateWith(capability: Capability<*>): State

        sealed interface Known : State
        sealed interface Unknown : State {
            val lastKnown: Known?
                get() = null

            override fun updateWith(capability: Capability<*>): State = this
        }
    }

    sealed interface Type<E : Entity>
}

val KClass<*>.formattedName: String get() = this.qualifiedName!!
    .replace(java.packageName, "")
    .trimStart('.')

fun Entity.State.unsupportedCapabilityError(capability: Capability<*>): Nothing =
    error("Unsupported '${capability::class.formattedName}' capability provided for '${this::class.formattedName}'")
