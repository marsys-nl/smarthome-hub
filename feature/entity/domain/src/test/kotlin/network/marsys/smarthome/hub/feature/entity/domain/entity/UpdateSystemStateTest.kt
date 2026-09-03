package network.marsys.smarthome.hub.feature.entity.domain.entity

import de.infix.testBalloon.framework.core.testSuite
import dev.nmarsman.expect.api.expectThat
import dev.nmarsman.expect.api.expectThrows
import dev.nmarsman.expect.assertions.hasMessage
import dev.nmarsman.expect.assertions.isA
import dev.nmarsman.expect.assertions.isEqualTo
import network.marsys.smarthome.domain.unit.Dimension
import network.marsys.smarthome.domain.unit.Quantity
import network.marsys.smarthome.domain.unit.celsius
import network.marsys.smarthome.domain.unit.gibibytes
import network.marsys.smarthome.domain.unit.gigabytes
import network.marsys.smarthome.domain.unit.percent
import network.marsys.smarthome.domain.unit.seconds
import network.marsys.smarthome.hub.feature.entity.domain.capability.Capability
import network.marsys.smarthome.hub.feature.entity.domain.capability.Capability.Companion.optional
import network.marsys.smarthome.hub.feature.entity.domain.capability.Capability.Companion.required
import network.marsys.smarthome.hub.feature.entity.domain.capability.Duration
import network.marsys.smarthome.hub.feature.entity.domain.capability.MeasuredDataSize
import network.marsys.smarthome.hub.feature.entity.domain.capability.MeasuredLoad
import network.marsys.smarthome.hub.feature.entity.domain.capability.MeasuredTemperature
import network.marsys.smarthome.hub.feature.entity.domain.capability.OnOff
import network.marsys.smarthome.hub.feature.entity.domain.capability.context.Application
import network.marsys.smarthome.hub.feature.entity.domain.capability.context.Host

val UpdateSystemStateTest by testSuite(
    name = "Update system state tests",
) {
    test(name = "Updating a known state with a valid measured data size when context matches memory type total") {
        val state: Entity.State = System.State.Known(
            info = host(),
            processor = processor(),
            memory = memory(),
            uptime = uptime(),
        )

        val update = MeasuredDataSize(current = 16.gibibytes) with System.MemoryType.Total

        expectThat(state.updateWith(update))
            .isA<System.State.Known>()
            .get(System.State.Known::memory)
            .get(System.Memory::total)
            .get(Capability.Present<MeasuredDataSize>::value)
            .get(MeasuredDataSize::current)
            .isEqualTo(16.gibibytes)
    }

    test(name = "Updating a known state with a valid measured data size when context matches memory type available") {
        val state: Entity.State = System.State.Known(
            info = host(),
            processor = processor(),
            memory = memory(),
            uptime = uptime(),
        )

        val update = MeasuredDataSize(current = 2.gibibytes) with System.MemoryType.Available

        expectThat(state.updateWith(update))
            .isA<System.State.Known>()
            .get(System.State.Known::memory)
            .get(System.Memory::available)
            .get(Capability.Present<MeasuredDataSize>::value)
            .get(MeasuredDataSize::current)
            .isEqualTo(2.gibibytes)
    }

    test(name = "Updating a known state with a valid measured data size when context matches memory type swap total") {
        val state: Entity.State = System.State.Known(
            info = host(),
            processor = processor(),
            memory = memory(),
            uptime = uptime(),
        )

        val update = MeasuredDataSize(current = 16.gibibytes) with System.MemoryType.SwapTotal

        expectThat(state.updateWith(update))
            .isA<System.State.Known>()
            .get(System.State.Known::memory)
            .get(System.Memory::swap)
            .get(System.Memory.Swap::total)
            .get(Capability.Present<MeasuredDataSize>::value)
            .get(MeasuredDataSize::current)
            .isEqualTo(16.gibibytes)
    }

    test(name = "Updating a known state with a valid measured data size when context matches memory type swap used") {
        val state: Entity.State = System.State.Known(
            info = host(),
            processor = processor(),
            memory = memory(),
            uptime = uptime(),
        )

        val update = MeasuredDataSize(current = 4.gibibytes) with System.MemoryType.SwapUsed

        expectThat(state.updateWith(update))
            .isA<System.State.Known>()
            .get(System.State.Known::memory)
            .get(System.Memory::swap)
            .get(System.Memory.Swap::used)
            .get(Capability.Present<MeasuredDataSize>::value)
            .get(MeasuredDataSize::current)
            .isEqualTo(4.gibibytes)
    }

    test(name = "Updating a known state with a valid measured data size without matching memory type fails") {
        val state: Entity.State = System.State.Known(
            info = host(),
            processor = processor(),
            memory = memory(),
            uptime = uptime(),
        )

        val update = MeasuredDataSize(current = 4.gibibytes) with Unsupported

        expectThrows<IllegalStateException> {
            state.updateWith(update)
        }.hasMessage("Unsupported 'MeasuredDataSize' capability provided for 'System.State.Known'")
    }

    test(name = "Updating a known state with a valid measured load") {
        val state: Entity.State = System.State.Known(
            info = host(),
            processor = processor(),
            memory = memory(),
            uptime = uptime(),
        )

        val update = MeasuredLoad(current = 75.percent)

        expectThat(state.updateWith(update))
            .isA<System.State.Known>()
            .get(System.State.Known::processor)
            .get(System.Processor::load)
            .get(Capability.Present<MeasuredLoad>::value)
            .get(MeasuredLoad::current)
            .isEqualTo(75.percent)
    }

    test(name = "Updating a known state with a valid measured temperature") {
        val state: Entity.State = System.State.Known(
            info = host(),
            processor = processor(),
            memory = memory(),
            uptime = uptime(),
        )

        val update = MeasuredTemperature(current = 75.celsius)

        expectThat(state.updateWith(update))
            .isA<System.State.Known>()
            .get(System.State.Known::processor)
            .get(System.Processor::temperature)
            .isA<Capability.Available<MeasuredTemperature>>()
            .get(Capability.Available<MeasuredTemperature>::value)
            .get(MeasuredTemperature::current)
            .isEqualTo(75.celsius)
    }

    test("Updating a known state with a valid required capability succeeds when context matches Application") {
        val state: Entity.State = System.State.Known(
            info = host(),
            processor = processor(),
            memory = memory(),
            uptime = uptime(),
        )

        val update = Duration(current = 10.seconds) with Application

        expectThat(state.updateWith(update))
            .isA<System.State.Known>()
            .get(System.State.Known::uptime)
            .get(System.Uptime::application)
            .get(Capability.Present<Duration>::value)
            .get(Duration::current)
            .isEqualTo(10.seconds)
    }

    test("Updating a known state with a valid capability only updates capability with matching context Application") {
        val state: Entity.State = System.State.Known(
            info = host(),
            processor = processor(),
            memory = memory(),
            uptime = uptime(),
        )

        val update = Duration(current = 10.seconds) with Application

        expectThat(state.updateWith(update))
            .isA<System.State.Known>()
            .get(System.State.Known::uptime)
            .get(System.Uptime::host)
            .get(Capability.Present<Duration>::value)
            .get(Duration::current)
            .isEqualTo(15.seconds)
    }

    test("Updating a known state with a valid required capability succeeds when context matches Host") {
        val state: Entity.State = System.State.Known(
            info = host(),
            processor = processor(),
            memory = memory(),
            uptime = uptime(),
        )

        val update = Duration(current = 10.seconds) with Host

        expectThat(state.updateWith(update))
            .isA<System.State.Known>()
            .get(System.State.Known::uptime)
            .get(System.Uptime::host)
            .get(Capability.Present<Duration>::value)
            .get(Duration::current)
            .isEqualTo(10.seconds)
    }

    test("Updating a known state with a valid capability only updates capability with matching context Host") {
        val state: Entity.State = System.State.Known(
            info = host(),
            processor = processor(),
            memory = memory(),
            uptime = uptime(),
        )

        val update = Duration(current = 10.seconds) with Host

        expectThat(state.updateWith(update))
            .isA<System.State.Known>()
            .get(System.State.Known::uptime)
            .get(System.Uptime::application)
            .get(Capability.Present<Duration>::value)
            .get(Duration::current)
            .isEqualTo(5.seconds)
    }

    test("Updating a known state with an unknown context fails") {
        val state: Entity.State = System.State.Known(
            info = host(),
            processor = processor(),
            memory = memory(),
            uptime = uptime(),
        )

        val update = Duration(current = 10.seconds) with Unsupported

        expectThrows<IllegalStateException> {
            state.updateWith(update)
        }.hasMessage("Unsupported 'Duration' capability provided for 'System.State.Known'")
    }

    test("Updating a known state with an unknown capability fails") {
        val state: Entity.State = System.State.Known(
            info = host(),
            processor = processor(),
            memory = memory(),
            uptime = uptime(),
        )

        val update = OnOff(current = true)

        expectThrows<IllegalStateException> {
            state.updateWith(update)
        }.hasMessage("Unsupported 'OnOff' capability provided for 'System.State.Known'")
    }
}

private fun host() = System.HostInfo(
    device = System.HostInfo.Device(
        manufacturer = "Test Manufacturer",
        model = "Test Model",
        architecture = "x86_64",
        physicalCores = 4,
        logicalCores = 8,
    ),
    operatingSystem = System.HostInfo.OperatingSystem(
        description = "Test OS",
        family = "Test Family",
        version = "1.0.0",
        bitness = 64,
    ),
)

private fun processor(
    load: Quantity<Dimension.Ratio> = 0.5.percent,
    temperature: Quantity<Dimension.Temperature> = 45.celsius,
) = System.Processor(
    load = required(MeasuredLoad(current = load)),
    temperature = optional(MeasuredTemperature(current = temperature)),
)

private fun memory(
    total: Quantity<Dimension.DigitalInformation> = 8.gibibytes,
    available: Quantity<Dimension.DigitalInformation> = 4.gibibytes,
    swapTotal: Quantity<Dimension.DigitalInformation> = 2.gibibytes,
    swapUsed: Quantity<Dimension.DigitalInformation> = 1.gibibytes,
) = System.Memory(
    total = required(MeasuredDataSize(current = total)),
    available = required(MeasuredDataSize(current = available)),
    swap = System.Memory.Swap(
        total = required(MeasuredDataSize(current = swapTotal)),
        used = required(MeasuredDataSize(current = swapUsed)),
    ),
)

private fun uptime(
    host: Quantity<Dimension.Time> = 15.seconds,
    application: Quantity<Dimension.Time> = 5.seconds,
) = System.Uptime(
    host = required(Duration(current = host)),
    application = required(Duration(current = application)),
)

private data object Unsupported : Capability.Context.Element {
    override val key: Capability.Context.Key<Unsupported>
        get() = Key

    data object Key : Capability.Context.Key<Unsupported>
}
