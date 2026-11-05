package network.marsys.smarthome.hub.feature.integration.infrastructure

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import network.marsys.smarthome.domain.TranslationKey
import network.marsys.smarthome.domain.capability.Brightness
import network.marsys.smarthome.domain.capability.Capability.Companion.optional
import network.marsys.smarthome.domain.capability.Capability.Companion.required
import network.marsys.smarthome.domain.capability.ChildLock
import network.marsys.smarthome.domain.capability.MeasureTemperature
import network.marsys.smarthome.domain.capability.OnOff
import network.marsys.smarthome.domain.capability.ScheduledMode
import network.marsys.smarthome.domain.capability.TargetTemperature
import network.marsys.smarthome.domain.capability.ThermostatMode
import network.marsys.smarthome.domain.capability.ThermostatMode.Mode
import network.marsys.smarthome.domain.capability.WindowDetection
import network.marsys.smarthome.domain.entity.Entity
import network.marsys.smarthome.domain.entity.EntityIdentifier
import network.marsys.smarthome.domain.entity.Light
import network.marsys.smarthome.domain.entity.Thermostat
import network.marsys.smarthome.domain.entity.naming
import network.marsys.smarthome.domain.identifiers.IntegrationIdentifier
import network.marsys.smarthome.domain.unit.celsius
import network.marsys.smarthome.domain.unit.percent
import network.marsys.smarthome.hub.feature.entity.domain.event.EntityDiscovered
import network.marsys.smarthome.hub.feature.entity.domain.event.EntityProvisioned
import network.marsys.smarthome.hub.feature.entity.domain.event.Event
import network.marsys.smarthome.hub.feature.integration.application.IntegrationAdapter
import network.marsys.smarthome.hub.feature.integration.application.IntegrationLifecycleController
import network.marsys.smarthome.hub.feature.integration.domain.Integration
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds

class FakeIntegrationAdapter(
    override val identifier: IntegrationIdentifier = IntegrationIdentifier("integration.fake"),
    initialStatus: Integration.Status = Integration.Status.Stopped,
) : IntegrationAdapter {
    private val entities = listOf<Entity>(
        Light(
            identifier = EntityIdentifier("light.dining-table"),
            naming = Light.naming(
                key = TranslationKey("light.living-room"),
                qualifier = TranslationKey("dining-table"),
            ),
            state = Light.State.Known(
                onOff = required(OnOff(current = true)),
                brightness = optional(Brightness(current = 78.percent)),
            ),
        ),
        Light(
            identifier = EntityIdentifier("light.kitchen"),
            naming = Light.naming(
                key = TranslationKey("light.kitchen"),
            ),
            state = Light.State.Known(
                onOff = required(OnOff(current = false)),
                brightness = optional(Brightness(current = 50.percent)),
            ),
        ),
        Light(
            identifier = EntityIdentifier("light.kitchen-spots"),
            naming = Light.naming(
                key = TranslationKey("light.kitchen"),
                qualifier = TranslationKey("spots"),
            ),
            state = Light.State.Known(
                onOff = required(OnOff(current = false)),
                brightness = optional(Brightness(current = 100.percent)),
            ),
        ),
        Light(
            identifier = EntityIdentifier("light.living-room"),
            naming = Light.naming(
                key = TranslationKey("light.living-room"),
                qualifier = TranslationKey("ceiling"),
            ),
            state = Light.State.Known(
                onOff = required(OnOff(current = false)),
                brightness = optional(Brightness(current = 100.percent)),
            ),
        ),
        Light(
            identifier = EntityIdentifier("light.living-room-spots"),
            naming = Light.naming(
                key = TranslationKey("light.living-room"),
                qualifier = TranslationKey("spots"),
            ),
            state = Light.State.Known(
                onOff = required(OnOff(current = false)),
                brightness = optional(Brightness(current = 50.percent)),
            ),
        ),

        Thermostat(
            identifier = EntityIdentifier("thermostat.bathroom"),
            naming = Thermostat.naming(
                key = TranslationKey("thermostat.bathroom"),
            ),
            state = Thermostat.State.Known(
                onOff = required(OnOff(current = true)),
                mode = required(ThermostatMode(current = Mode.Heat)),
                temperatures = Thermostat.Temperatures(
                    target = required(TargetTemperature(current = 18.celsius)),
                    current = required(MeasureTemperature(current = 18.4.celsius)),
                    outdoor = optional(null),
                ),
                childLock = optional(ChildLock(current = false)),
                windowDetection = optional(WindowDetection(current = true)),
                scheduledMode = optional(ScheduledMode(current = false)),
            ),
        ),
        Thermostat(
            identifier = EntityIdentifier("thermostat.front-bedroom"),
            naming = Thermostat.naming(
                key = TranslationKey("thermostat.bedroom"),
            ),
            state = Thermostat.State.Known(
                onOff = required(OnOff(current = true)),
                mode = required(ThermostatMode(current = Mode.Heat)),
                temperatures = Thermostat.Temperatures(
                    target = required(TargetTemperature(current = 18.celsius)),
                    current = required(MeasureTemperature(current = 18.9.celsius)),
                    outdoor = optional(null),
                ),
                childLock = optional(ChildLock(current = true)),
                windowDetection = optional(WindowDetection(current = true)),
                scheduledMode = optional(ScheduledMode(current = false)),
            ),
        ),
        Thermostat(
            identifier = EntityIdentifier("thermostat.master-bedroom"),
            naming = Thermostat.naming(
                key = TranslationKey("thermostat.main-bedroom"),
            ),
            state = Thermostat.State.Known(
                onOff = required(OnOff(current = true)),
                mode = required(ThermostatMode(current = Mode.Heat)),
                temperatures = Thermostat.Temperatures(
                    target = required(TargetTemperature(current = 18.celsius)),
                    current = required(MeasureTemperature(current = 19.3.celsius)),
                    outdoor = optional(null),
                ),
                childLock = optional(ChildLock(current = false)),
                windowDetection = optional(WindowDetection(current = true)),
                scheduledMode = optional(ScheduledMode(current = false)),
            ),
        ),
        Thermostat(
            identifier = EntityIdentifier("thermostat.nursery"),
            naming = Thermostat.naming(
                key = TranslationKey("thermostat.nursery"),
            ),
            state = Thermostat.State.Known(
                onOff = required(OnOff(current = true)),
                mode = required(ThermostatMode(current = Mode.Heat)),
                temperatures = Thermostat.Temperatures(
                    target = required(TargetTemperature(current = 18.celsius)),
                    current = required(MeasureTemperature(current = 18.6.celsius)),
                    outdoor = optional(null),
                ),
                childLock = optional(ChildLock(current = true)),
                windowDetection = optional(WindowDetection(current = true)),
                scheduledMode = optional(ScheduledMode(current = false)),
            ),
        ),
        Thermostat(
            identifier = EntityIdentifier("thermostat.office"),
            naming = Thermostat.naming(
                key = TranslationKey("thermostat.office"),
            ),
            state = Thermostat.State.Known(
                onOff = required(OnOff(current = true)),
                mode = required(ThermostatMode(current = Mode.Heat)),
                temperatures = Thermostat.Temperatures(
                    target = required(TargetTemperature(current = 21.celsius)),
                    current = required(MeasureTemperature(current = 20.5.celsius)),
                    outdoor = optional(null),
                ),
                childLock = optional(ChildLock(current = false)),
                windowDetection = optional(WindowDetection(current = true)),
                scheduledMode = optional(ScheduledMode(current = false)),
            ),
        ),
    )

    private val lifecycle = IntegrationLifecycleController(
        initialStatus = initialStatus,
        onStart = {
            eventChannel.trySendProvisioned(entities = entities)
            eventChannel.trySendDiscovered(entities = entities)
        },
        onStop = {
            delay(2.seconds)
        },
    )

    private val eventChannel = Channel<Event>(Channel.BUFFERED)
    override val events: Flow<Event> = eventChannel.receiveAsFlow()

    override val status: StateFlow<Integration.Status> = lifecycle.status

    override suspend fun start() = lifecycle.start()
    override suspend fun stop() = lifecycle.stop()

    private fun Channel<Event>.trySendProvisioned(entities: Collection<Entity>) {
        val now = Clock.System.now()

        entities.forEach { entity ->
            trySend(
                EntityProvisioned(
                    identifier = entity.identifier,
                    type = entity.type,
                    naming = entity.naming,
                    occurredAt = now,
                ),
            )
        }
    }

    private fun Channel<Event>.trySendDiscovered(entities: Collection<Entity>) {
        val now = Clock.System.now()

        entities
            .filter { it.state is Entity.State.Known }
            .forEach { entity ->
                trySend(
                    EntityDiscovered(
                        identifier = entity.identifier,
                        state = entity.state,
                        occurredAt = now,
                    ),
                )
            }
    }
}
