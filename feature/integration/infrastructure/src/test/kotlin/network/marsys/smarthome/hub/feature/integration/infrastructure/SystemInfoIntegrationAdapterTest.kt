package network.marsys.smarthome.hub.feature.integration.infrastructure

import app.cash.turbine.test
import de.infix.testBalloon.framework.core.testSuite
import dev.nmarsman.expect.api.expectThat
import dev.nmarsman.expect.assertions.isA
import dev.nmarsman.expect.assertions.isEqualTo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import network.marsys.smarthome.domain.unit.bytes
import network.marsys.smarthome.domain.unit.celsius
import network.marsys.smarthome.domain.unit.percent
import network.marsys.smarthome.hub.feature.entity.domain.capability.Capability
import network.marsys.smarthome.hub.feature.entity.domain.capability.MeasuredDataSize
import network.marsys.smarthome.hub.feature.entity.domain.capability.MeasuredLoad
import network.marsys.smarthome.hub.feature.entity.domain.capability.MeasuredTemperature
import network.marsys.smarthome.hub.feature.entity.domain.entity.System
import network.marsys.smarthome.hub.feature.entity.domain.event.CapabilityUpdated
import network.marsys.smarthome.hub.feature.entity.domain.event.EntityDiscovered
import network.marsys.smarthome.hub.feature.entity.domain.event.EntityProvisioned
import network.marsys.smarthome.hub.feature.integration.domain.Integration
import oshi.SystemInfo
import oshi.hardware.Baseboard
import oshi.hardware.CentralProcessor
import oshi.hardware.ComputerSystem
import oshi.hardware.Firmware
import oshi.hardware.GlobalMemory
import oshi.hardware.GraphicsCard
import oshi.hardware.HWDiskStore
import oshi.hardware.HardwareAbstractionLayer
import oshi.hardware.NetworkIF
import oshi.hardware.PhysicalMemory
import oshi.hardware.PowerSource
import oshi.hardware.Sensors
import oshi.hardware.VirtualMemory
import oshi.software.os.FileSystem
import oshi.software.os.InternetProtocolStats
import oshi.software.os.NetworkParams
import oshi.software.os.OSProcess
import oshi.software.os.OSThread
import oshi.software.os.OperatingSystem
import java.util.function.Predicate
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
val SystemInfoIntegrationAdapterTest by testSuite(
    name = "SystemInfoIntegrationAdapter tests",
) {
    test("SystemInfoIntegrationAdapter should be able to start") {
        runTest {
            val adapter = SystemInfoIntegrationAdapter(
                scope = backgroundScope,
            )

            adapter.events.test {
                adapter.start()

                expectThat(awaitItem())
                    .isA<EntityProvisioned>()
                    .get(EntityProvisioned::type)
                    .isEqualTo(System)

                expectThat(awaitItem())
                    .isA<EntityDiscovered>()
            }
        }
    }

    test("SystemInfoIntegrationAdapter should be able to stop when started") {
        runTest {
            val adapter = SystemInfoIntegrationAdapter(
                scope = backgroundScope,
            )

            adapter.events.test {
                adapter.start()

                expectThat(awaitItem())
                    .isA<EntityProvisioned>()
                    .get(EntityProvisioned::type)
                    .isEqualTo(System)

                expectThat(awaitItem())
                    .isA<EntityDiscovered>()

                adapter.stop()

                expectNoEvents()
            }
        }
    }

    test("SystemInfoIntegrationAdapter should be able to stop when not started") {
        runTest {
            val adapter = SystemInfoIntegrationAdapter(
                scope = backgroundScope,
            )

            adapter.events.test {
                adapter.stop()

                expectNoEvents()
            }
        }
    }

    test("SystemInfoIntegrationAdapter should be able to stop when running") {
        runTest {
            val adapter = SystemInfoIntegrationAdapter(
                initialStatus = Integration.Status.Running,
                scope = backgroundScope,
            )

            adapter.events.test {
                adapter.stop()

                expectNoEvents()
            }
        }
    }

    test("SystemInfoIntegrationAdapter sets cpu temperature correctly") {
        runTest {
            val adapter = SystemInfoIntegrationAdapter(
                applicationStartedAt = Clock.System.now().epochSeconds,
                systemInfo = FakeSystemInfo(
                    hardware = FakeHardwareAbstractionLayer(
                        sensors = FakeSensors(
                            temperature = 56.0,
                        ),
                    ),
                ),
                scope = backgroundScope,
            )

            adapter.events.test {
                adapter.start()

                expectThat(awaitItem())
                    .isA<EntityProvisioned>()
                    .get(EntityProvisioned::type)
                    .isEqualTo(System)

                expectThat(awaitItem())
                    .isA<EntityDiscovered>()
                    .get(EntityDiscovered::state)
                    .isA<System.State.Known>()
                    .get(System.State.Known::processor)
                    .get(System.Processor::temperature)
                    .isA<Capability.Available<MeasuredTemperature>>()
                    .get(Capability.Available<MeasuredTemperature>::value)
                    .get(MeasuredTemperature::current)
                    .isEqualTo(56.celsius)

                expectNoEvents()
            }
        }
    }

    test("SystemInfoIntegrationAdapter sets cpu temperature as unsupported if no temperature is provided") {
        runTest {
            val adapter = SystemInfoIntegrationAdapter(
                applicationStartedAt = Clock.System.now().epochSeconds,
                systemInfo = FakeSystemInfo(
                    hardware = FakeHardwareAbstractionLayer(
                        sensors = FakeSensors(
                            temperature = 0.0,
                        ),
                    ),
                ),
                scope = backgroundScope,
            )

            adapter.events.test {
                adapter.start()

                expectThat(awaitItem())
                    .isA<EntityProvisioned>()
                    .get(EntityProvisioned::type)
                    .isEqualTo(System)

                expectThat(awaitItem())
                    .isA<EntityDiscovered>()
                    .get(EntityDiscovered::state)
                    .isA<System.State.Known>()
                    .get(System.State.Known::processor)
                    .get(System.Processor::temperature)
                    .isEqualTo(Capability.Unsupported)

                expectNoEvents()
            }
        }
    }

    test("SystemInfoIntegrationAdapter sends processor updates") {
        runTest {
            val adapter = SystemInfoIntegrationAdapter(
                applicationStartedAt = Clock.System.now().epochSeconds,
                systemInfo = FakeSystemInfo(
                    hardware = FakeHardwareAbstractionLayer(
                        processor = FakeCentralProcessor(
                            load = .5,
                        ),
                        sensors = FakeSensors(
                            temperature = 56.0,
                        ),
                    ),
                ),
                scope = backgroundScope,
            )

            adapter.events.test(timeout = 10.seconds) {
                adapter.start()

                expectThat(awaitItem()).isA<EntityProvisioned>()
                expectThat(awaitItem()).isA<EntityDiscovered>()

                expectThat(awaitItem())
                    .isA<CapabilityUpdated>()
                    .get(CapabilityUpdated::capability)
                    .isA<MeasuredLoad>()
                    .get(MeasuredLoad::current)
                    .isEqualTo(50.percent)

                expectThat(awaitItem())
                    .isA<CapabilityUpdated>()
                    .get(CapabilityUpdated::capability)
                    .isA<MeasuredTemperature>()
                    .get(MeasuredTemperature::current)
                    .isEqualTo(56.celsius)

                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    test("SystemInfoIntegrationAdapter doesnt send processor temperature if it doesn't have one") {
        runTest {
            val adapter = SystemInfoIntegrationAdapter(
                applicationStartedAt = Clock.System.now().epochSeconds,
                systemInfo = FakeSystemInfo(
                    hardware = FakeHardwareAbstractionLayer(
                        processor = FakeCentralProcessor(
                            load = .5,
                        ),
                        sensors = FakeSensors(
                            temperature = 0.0,
                        ),
                    ),
                ),
                scope = backgroundScope,
            )

            adapter.events.test(timeout = 10.seconds) {
                adapter.start()

                expectThat(awaitItem()).isA<EntityProvisioned>()
                expectThat(awaitItem()).isA<EntityDiscovered>()

                expectThat(awaitItem())
                    .isA<CapabilityUpdated>()
                    .get(CapabilityUpdated::capability)
                    .isA<MeasuredLoad>()
                    .get(MeasuredLoad::current)
                    .isEqualTo(50.percent)

                expectThat(awaitItem())
                    .isA<CapabilityUpdated>()
                    .get(CapabilityUpdated::capability)
                    .isA<MeasuredDataSize>()

                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    test("SystemInfoIntegrationAdapter sends memory updates") {
        runTest {
            val adapter = SystemInfoIntegrationAdapter(
                applicationStartedAt = Clock.System.now().epochSeconds,
                systemInfo = FakeSystemInfo(
                    hardware = FakeHardwareAbstractionLayer(
                        globalMemory = FakeGlobalMemory(
                            available = 512,
                            virtualMemory = FakeVirtualMemory(
                                used = 1536,
                            ),
                        ),
                    ),
                ),
                scope = backgroundScope,
            )

            adapter.events.test(timeout = 10.seconds) {
                adapter.start()

                expectThat(awaitItem()).isA<EntityProvisioned>()
                expectThat(awaitItem()).isA<EntityDiscovered>()

                skipItems(2)

                expectThat(awaitItem())
                    .isA<CapabilityUpdated>()
                    .get(CapabilityUpdated::capability)
                    .isA<MeasuredDataSize>()
                    .get(MeasuredDataSize::current)
                    .isEqualTo(512.bytes)

                expectThat(awaitItem())
                    .isA<CapabilityUpdated>()
                    .get(CapabilityUpdated::capability)
                    .isA<MeasuredDataSize>()
                    .get(MeasuredDataSize::current)
                    .isEqualTo(1536.bytes)

                cancelAndIgnoreRemainingEvents()
            }
        }
    }
}

private class FakeSystemInfo(
    private val hardware: HardwareAbstractionLayer = FakeHardwareAbstractionLayer(),
    private val operatingSystem: OperatingSystem = FakeOperatingSystem(),
) : SystemInfo() {
    override fun getHardware(): HardwareAbstractionLayer = hardware
    override fun getOperatingSystem(): OperatingSystem = operatingSystem
}

private class FakeHardwareAbstractionLayer(
    private val computerSystem: ComputerSystem? = FakeComputerSystem(),
    private val processor: CentralProcessor? = FakeCentralProcessor(),
    private val globalMemory: GlobalMemory? = FakeGlobalMemory(),
    private val sensors: Sensors? = FakeSensors(),
) : HardwareAbstractionLayer {
    override fun getComputerSystem(): ComputerSystem? = computerSystem
    override fun getProcessor(): CentralProcessor? = processor
    override fun getMemory(): GlobalMemory? = globalMemory
    override fun getSensors(): Sensors? = sensors

    override fun getPowerSources(): List<PowerSource?>? = null
    override fun getDiskStores(): List<HWDiskStore?>? = null
    override fun getNetworkIFs(): List<NetworkIF?>? = null
    override fun getNetworkIFs(includeLocalInterfaces: Boolean) = null
    override fun getDisplays() = null
    override fun getUsbDevices(tree: Boolean) = null
    override fun getSoundCards() = null
    override fun getGraphicsCards() = emptyList<GraphicsCard?>()
}

private class FakeComputerSystem : ComputerSystem {
    override fun getManufacturer(): String = "Manufacturer"
    override fun getModel(): String = "Model"

    override fun getSerialNumber(): String? = null
    override fun getHardwareUUID(): String? = null
    override fun getFirmware(): Firmware? = null
    override fun getBaseboard(): Baseboard? = null
}

private class FakeCentralProcessor(
    private val load: Double = 1.0,
) : CentralProcessor {
    override fun getProcessorIdentifier(): CentralProcessor.ProcessorIdentifier =
        CentralProcessor.ProcessorIdentifier("Vendor", "Name", "Family", "Model", "Stepping", "Identifier", true)

    override fun getMaxFreq(): Long = 2600
    override fun getCurrentFreq(): LongArray = longArrayOf()

    override fun getLogicalProcessors(): List<CentralProcessor.LogicalProcessor?>? = null
    override fun getPhysicalProcessors(): List<CentralProcessor.PhysicalProcessor?>? = null
    override fun getProcessorCaches(): List<CentralProcessor.ProcessorCache?>? = null
    override fun getFeatureFlags(): List<String?>? = null

    override fun getSystemCpuLoadBetweenTicks(oldTicks: LongArray?): Double = load
    override fun getSystemCpuLoadTicks(): LongArray = longArrayOf()
    override fun getSystemLoadAverage(nelem: Int): DoubleArray? = null
    override fun getProcessorCpuLoadBetweenTicks(oldTicks: Array<out LongArray?>?): DoubleArray? = null
    override fun getProcessorCpuLoadTicks(): Array<out LongArray?>? = null

    override fun getLogicalProcessorCount(): Int = 32
    override fun getPhysicalProcessorCount(): Int = 8
    override fun getPhysicalPackageCount(): Int = 0
    override fun getContextSwitches(): Long = 0
    override fun getInterrupts(): Long = 0
}

private class FakeGlobalMemory(
    private val total: Long = 1024,
    private val available: Long = 256,
    private val virtualMemory: VirtualMemory? = FakeVirtualMemory(),
) : GlobalMemory {
    override fun getTotal(): Long = total
    override fun getAvailable(): Long = available
    override fun getPageSize(): Long = 16

    override fun getVirtualMemory(): VirtualMemory? = virtualMemory
    override fun getPhysicalMemory(): List<PhysicalMemory?>? = null
}

private class FakeVirtualMemory(
    private val total: Long = 2048,
    private val used: Long = 1024,
) : VirtualMemory {
    override fun getSwapTotal(): Long = total
    override fun getSwapUsed(): Long = used

    override fun getVirtualMax(): Long = 1000
    override fun getVirtualInUse(): Long = 500

    override fun getSwapPagesIn(): Long = 10
    override fun getSwapPagesOut(): Long = 5
}

private class FakeSensors(
    val temperature: Double = 56.0,
) : Sensors {
    override fun getCpuTemperature(): Double = temperature
    override fun getFanSpeeds(): IntArray? = null
    override fun getCpuVoltage(): Double = 0.0
}

private class FakeOperatingSystem : OperatingSystem {
    override fun getFamily(): String = "Family"
    override fun getManufacturer(): String = "Manufacturer"
    override fun getVersionInfo(): OperatingSystem.OSVersionInfo =
        OperatingSystem.OSVersionInfo("1.0.0", "code", "build")

    override fun getFileSystem(): FileSystem? = null

    override fun getInternetProtocolStats(): InternetProtocolStats? = null

    override fun getProcesses(filter: Predicate<OSProcess?>?, sort: Comparator<OSProcess?>?, limit: Int): List<OSProcess?>? = null
    override fun getProcess(pid: Int): OSProcess? = null

    override fun getChildProcesses(
        parentPid: Int,
        filter: Predicate<OSProcess?>?,
        sort: Comparator<OSProcess?>?,
        limit: Int,
    ): List<OSProcess?>? = null

    override fun getDescendantProcesses(
        parentPid: Int,
        filter: Predicate<OSProcess?>?,
        sort: Comparator<OSProcess?>?,
        limit: Int,
    ): List<OSProcess?>? = null

    override fun getProcessId(): Int = 655354
    override fun getProcessCount(): Int = 14
    override fun getThreadId(): Int = 1337

    override fun getCurrentThread(): OSThread? = null

    override fun getThreadCount(): Int = 8
    override fun getBitness(): Int = 64

    override fun getSystemUptime(): Long = 124124
    override fun getSystemBootTime(): Long = 124124

    override fun getNetworkParams(): NetworkParams? = null
}
