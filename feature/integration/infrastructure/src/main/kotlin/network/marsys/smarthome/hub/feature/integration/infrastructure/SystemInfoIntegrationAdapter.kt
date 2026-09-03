package network.marsys.smarthome.hub.feature.integration.infrastructure

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import network.marsys.smarthome.domain.identifiers.EntityIdentifier
import network.marsys.smarthome.domain.identifiers.IntegrationIdentifier
import network.marsys.smarthome.domain.unit.celsius
import network.marsys.smarthome.domain.unit.gibibytes
import network.marsys.smarthome.domain.unit.percent
import network.marsys.smarthome.domain.unit.seconds
import network.marsys.smarthome.hub.feature.entity.domain.capability.Capability.Companion.optional
import network.marsys.smarthome.hub.feature.entity.domain.capability.Capability.Companion.required
import network.marsys.smarthome.hub.feature.entity.domain.capability.Duration
import network.marsys.smarthome.hub.feature.entity.domain.capability.MeasuredDataSize
import network.marsys.smarthome.hub.feature.entity.domain.capability.MeasuredLoad
import network.marsys.smarthome.hub.feature.entity.domain.capability.MeasuredTemperature
import network.marsys.smarthome.hub.feature.entity.domain.entity.System
import network.marsys.smarthome.hub.feature.entity.domain.event.EntityDiscovered
import network.marsys.smarthome.hub.feature.entity.domain.event.EntityProvisioned
import network.marsys.smarthome.hub.feature.entity.domain.event.Event
import network.marsys.smarthome.hub.feature.integration.application.IntegrationAdapter
import network.marsys.smarthome.hub.feature.integration.application.IntegrationLifecycleController
import network.marsys.smarthome.hub.feature.integration.domain.Integration
import oshi.SystemInfo
import oshi.hardware.HardwareAbstractionLayer
import oshi.software.os.OperatingSystem
import java.lang.management.ManagementFactory
import kotlin.time.Duration.Companion.milliseconds

class SystemInfoIntegrationAdapter(
    override val identifier: IntegrationIdentifier = IntegrationIdentifier("integration.system"),
    initialStatus: Integration.Status = Integration.Status.Stopped,
    private val applicationStartedAt: Long = ManagementFactory.getRuntimeMXBean().startTime,
    private val systemInfo: SystemInfo = SystemInfo(),
) : IntegrationAdapter {
    private val entityIdentifier = EntityIdentifier("smarthome.system")

    private val scope = CoroutineScope(SupervisorJob())
    private var job: Job? = null

    private val lifecycle = IntegrationLifecycleController(
        initialStatus = initialStatus,
        onStart = {
            job = scope.launch {
                initialize()
            }
        },
        onStop = {
            job?.cancel()
            job = null
        },
    )

    private val eventChannel = Channel<Event>(Channel.BUFFERED)
    override val events: Flow<Event> = eventChannel.receiveAsFlow()

    override val status: StateFlow<Integration.Status> = lifecycle.status

    override suspend fun start() = lifecycle.start()
    override suspend fun stop() = lifecycle.stop()

    private suspend fun initialize() {
        eventChannel.trySend(
            EntityProvisioned(
                identifier = entityIdentifier,
                type = System,
            ),
        )

        eventChannel.trySend(
            EntityDiscovered(
                identifier = entityIdentifier,
                state = systemState(),
            ),
        )
    }

    private suspend fun systemState(): System.State = withContext(Dispatchers.IO) {
        val applicationUptime = (java.lang.System.currentTimeMillis() - applicationStartedAt).seconds

        return@withContext context(with = systemInfo.hardware) {
            System.State.Known(
                info = context(with = systemInfo.operatingSystem) {
                    constructHostInfo()
                },
                processor = constructProcessorInfo(),
                memory = constructMemoryInfo(),
                uptime = System.Uptime(
                    host = required(Duration(current = systemInfo.operatingSystem.systemUptime.seconds)),
                    application = required(Duration(current = applicationUptime)),
                ),
            )
        }
    }

    context(hardware: HardwareAbstractionLayer)
    private suspend fun constructProcessorInfo(): System.Processor = System.Processor(
        load = required(measureProcessorLoad()),
        temperature = optional(measureProcessorTemperature()),
    )

    context(hardware: HardwareAbstractionLayer)
    private suspend fun measureProcessorLoad(
        interval: kotlin.time.Duration = 1000.milliseconds,
    ): MeasuredLoad = withContext(Dispatchers.IO) {
        val previousTicks = hardware.processor.systemCpuLoadTicks
        delay(interval)
        MeasuredLoad(
            current = hardware.processor
                .getSystemCpuLoadBetweenTicks(previousTicks)
                .let { (it * 100).percent },
        )
    }

    context(hardware: HardwareAbstractionLayer)
    private fun measureProcessorTemperature(): MeasuredTemperature? =
        hardware.sensors.cpuTemperature
            .takeIf { it > 0.0 }
            ?.let { MeasuredTemperature(current = it.celsius) }

    context(hardware: HardwareAbstractionLayer)
    private suspend fun constructMemoryInfo(): System.Memory = System.Memory(
        total = required(MeasuredDataSize(hardware.memory.total.gibibytes)),
        available = required(MeasuredDataSize(hardware.memory.available.gibibytes)),
        swap = System.Memory.Swap(
            total = required(MeasuredDataSize(hardware.memory.virtualMemory.swapTotal.gibibytes)),
            used = required(MeasuredDataSize(hardware.memory.virtualMemory.swapUsed.gibibytes)),
        ),
    )

    context(hardware: HardwareAbstractionLayer, operatingSystem: OperatingSystem)
    private fun constructHostInfo(): System.HostInfo = System.HostInfo(
        device = System.HostInfo.Device(
            manufacturer = hardware.computerSystem.manufacturer,
            model = hardware.computerSystem.model,
            architecture = hardware.processor.processorIdentifier.microarchitecture,
            physicalCores = hardware.processor.physicalProcessorCount,
            logicalCores = hardware.processor.logicalProcessorCount,
        ),
        operatingSystem = System.HostInfo.OperatingSystem(
            description = operatingSystem.toString(),
            family = operatingSystem.family,
            version = operatingSystem.versionInfo.version,
            bitness = operatingSystem.bitness,
        ),
    )

    companion object {
        private val interval = 5000.milliseconds
    }
}
