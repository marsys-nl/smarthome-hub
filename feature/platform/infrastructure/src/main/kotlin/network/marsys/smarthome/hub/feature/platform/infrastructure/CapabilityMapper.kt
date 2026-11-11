package network.marsys.smarthome.hub.feature.platform.infrastructure

import network.marsys.smarthome.api.mappers.Mapper
import network.marsys.smarthome.api.models.platform.CapabilityMessage
import network.marsys.smarthome.domain.capability.Brightness
import network.marsys.smarthome.domain.capability.Capability
import network.marsys.smarthome.domain.capability.ChildLock
import network.marsys.smarthome.domain.capability.MeasureDataSize
import network.marsys.smarthome.domain.capability.MeasureLoad
import network.marsys.smarthome.domain.capability.MeasureTemperature
import network.marsys.smarthome.domain.capability.Movement
import network.marsys.smarthome.domain.capability.OnOff
import network.marsys.smarthome.domain.capability.Opened
import network.marsys.smarthome.domain.capability.Position
import network.marsys.smarthome.domain.capability.ScheduledMode
import network.marsys.smarthome.domain.capability.TargetTemperature
import network.marsys.smarthome.domain.capability.ThermostatMode
import network.marsys.smarthome.domain.capability.ThermostatStatus
import network.marsys.smarthome.domain.capability.WindowDetection
import network.marsys.smarthome.domain.unit.Quantity

internal object CapabilityMapper : Mapper<Capability<*, *>, CapabilityMessage> {
    override fun invoke(source: Capability<*, *>): CapabilityMessage =
        when (val current = source.current) {
            is Boolean -> CapabilityMessage.BooleanValue(
                type = source.mapped,
                context = ContextMapper.invoke(source = source.context),
                current = current,
            )

            is Quantity<*> -> CapabilityMessage.DoubleValue(
                type = source.mapped,
                context = ContextMapper.invoke(source = source.context),
                current = current.value,
            )

            else -> CapabilityMessage.EnumValue(
                type = source.mapped,
                context = ContextMapper.invoke(source = source.context),
                current = current.toString(),
            )
        }

    private val Capability<*, *>.mapped
        get() = when (this) {
            is Brightness -> "brightness"
            is ChildLock -> "childLock"
            is MeasureDataSize -> "measureDataSize"
            is MeasureLoad -> "measureLoad"
            is MeasureTemperature -> "measureTemperature"
            is Movement -> "movement"
            is OnOff -> "onOff"
            is Opened -> "opened"
            is Position -> "position"
            is ScheduledMode -> "scheduledMode"
            is TargetTemperature -> "targetTemperature"
            is ThermostatMode -> "thermostatMode"
            is ThermostatStatus -> "thermostatStatus"
            is WindowDetection -> "windowDetection"
        }
}
