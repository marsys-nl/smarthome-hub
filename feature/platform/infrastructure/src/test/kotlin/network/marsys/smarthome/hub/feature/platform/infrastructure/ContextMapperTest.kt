package network.marsys.smarthome.hub.feature.platform.infrastructure

import de.infix.testBalloon.framework.core.testSuite
import dev.nmarsman.expect.api.expectThat
import dev.nmarsman.expect.api.expectThrows
import dev.nmarsman.expect.assertions.containsExactly
import dev.nmarsman.expect.assertions.hasMessage
import dev.nmarsman.expect.assertions.hasSize
import dev.nmarsman.expect.assertions.isA
import dev.nmarsman.expect.assertions.isEmpty
import dev.nmarsman.expect.assertions.isEqualTo
import dev.nmarsman.expect.assertions.single
import network.marsys.smarthome.api.models.platform.ContextElementMessage
import network.marsys.smarthome.api.models.platform.ContextMessage
import network.marsys.smarthome.domain.capability.Capability
import network.marsys.smarthome.domain.entity.System
import network.marsys.smarthome.domain.entity.Thermostat
import network.marsys.smarthome.domain.entity.formattedName

val ContextMapperTest by testSuite(
    name = "Context mapper tests",
) {
    test(name = "Mapping an empty context results in an empty context message") {
        val context = Capability.Context.Empty

        expectThat(ContextMapper.invoke(context))
            .isA<ContextMessage>()
            .get(ContextMessage::elements)
            .isEmpty()
    }

    mapOf(
        System.MemoryType.Available to ContextElementMessage.MemoryType.Available,
        System.MemoryType.Total to ContextElementMessage.MemoryType.Total,
        System.MemoryType.SwapAvailable to ContextElementMessage.MemoryType.SwapAvailable,
        System.MemoryType.SwapTotal to ContextElementMessage.MemoryType.SwapTotal,
        Thermostat.TemperatureType.Current to ContextElementMessage.TemperatureType.Current,
        Thermostat.TemperatureType.Outdoor to ContextElementMessage.TemperatureType.Outdoor,
    ).forEach { (original, expected) ->
        test(
            name = "Mapping context element '${original::class.formattedName}' results in " +
                "a context element message of type ${expected::class.formattedName}",
        ) {
            val context = Capability.Context.Empty + original

            expectThat(ContextMapper.invoke(context))
                .isA<ContextMessage>()
                .get(ContextMessage::elements)
                .single()
                .isEqualTo(expected)
        }
    }

    test(name = "Mapping multiple context elements results in a context message that contains all context elements - different types") {
        val context = Capability.Context.Empty + System.MemoryType.Total + Thermostat.TemperatureType.Outdoor

        expectThat(ContextMapper.invoke(context))
            .isA<ContextMessage>()
            .get(ContextMessage::elements)
            .hasSize(2)
            .containsExactly(
                ContextElementMessage.MemoryType.Total,
                ContextElementMessage.TemperatureType.Outdoor,
            )
    }

    test(name = "Mapping multiple context elements results in a context message that contains all context elements - same type") {
        val context = Capability.Context.Empty + System.MemoryType.Total + System.MemoryType.Available

        expectThat(ContextMapper.invoke(context))
            .isA<ContextMessage>()
            .get(ContextMessage::elements)
            .hasSize(2)
            .containsExactly(
                ContextElementMessage.MemoryType.Total,
                ContextElementMessage.MemoryType.Available,
            )
    }

    test(name = "Mapping an unsupported context element results in an error") {
        val context = Capability.Context.Empty + Unsupported

        expectThrows<IllegalStateException> { ContextMapper.invoke(context) }
            .hasMessage("Unsupported context element type: Unsupported")
    }
}

private data object Unsupported : Capability.Context.Element, Capability.Context.Key<Unsupported> {
    override val key: Capability.Context.Key<Unsupported> = Unsupported
}
