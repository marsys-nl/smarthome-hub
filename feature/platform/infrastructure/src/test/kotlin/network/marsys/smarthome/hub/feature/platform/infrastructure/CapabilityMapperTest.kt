package network.marsys.smarthome.hub.feature.platform.infrastructure

import de.infix.testBalloon.framework.core.testSuite
import dev.nmarsman.expect.api.expectThat
import dev.nmarsman.expect.assertions.isA
import dev.nmarsman.expect.assertions.isEqualTo
import network.marsys.smarthome.api.models.platform.CapabilityMessage
import network.marsys.smarthome.domain.capability.Brightness
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
import network.marsys.smarthome.domain.entity.formattedName
import network.marsys.smarthome.domain.unit.Quantity
import network.marsys.smarthome.domain.unit.celsius
import network.marsys.smarthome.domain.unit.gigabytes
import network.marsys.smarthome.domain.unit.percent

val CapabilityMapperTest by testSuite(
    name = "Capability mapper tests",
) {
    val examples = mapOf(
        "brightness" to Brightness(current = 50.percent),
        "childLock" to ChildLock(current = true),
        "measureDataSize" to MeasureDataSize(current = 10.gigabytes),
        "measureLoad" to MeasureLoad(current = 10.percent),
        "measureTemperature" to MeasureTemperature(current = 21.celsius),
        "movement" to Movement(current = Movement.Direction.Idle),
        "onOff" to OnOff(current = true),
        "opened" to Opened(current = true),
        "position" to Position(current = 75.percent),
        "scheduledMode" to ScheduledMode(current = false),
        "targetTemperature" to TargetTemperature(current = 21.celsius),
        "thermostatMode" to ThermostatMode(current = ThermostatMode.Mode.Heat),
        "thermostatStatus" to ThermostatStatus(current = ThermostatStatus.Status.Off),
        "windowDetection" to WindowDetection(current = false),
    )

    testSuite(
        name = "Mapping a capability results in expected identifier",
    ) {
        examples.forEach { (identifier, capability) ->
            test(name = "Mapping a '${capability::class.formattedName}' will result in type '$identifier'") {
                expectThat(CapabilityMapper.invoke(source = capability))
                    .get(CapabilityMessage::type)
                    .isEqualTo(identifier)
            }
        }
    }

    testSuite(
        name = "Mapping a capability will result in a boolean value",
    ) {
        examples
            .filter { it.value.current is Boolean }
            .forEach { (_, capability) ->
                test(name = "Mapping a '${capability::class.formattedName}' will result in a boolean value") {
                    expectThat(CapabilityMapper.invoke(source = capability))
                        .isA<CapabilityMessage.BooleanValue>()
                }
            }
    }

    testSuite(
        name = "Mapping a capability will result in a double value",
    ) {
        examples
            .filter { it.value.current is Quantity<*> }
            .forEach { (_, capability) ->
                test(name = "Mapping a '${capability::class.formattedName}' will result in a double value") {
                    expectThat(CapabilityMapper.invoke(source = capability))
                        .isA<CapabilityMessage.DoubleValue>()
                }
            }
    }

    testSuite(
        name = "Mapping a capability will result in a enum value",
    ) {
        examples
            .filter { it.value.current is Enum<*> }
            .forEach { (_, capability) ->
                test(name = "Mapping a '${capability::class.formattedName}' will result in a enum value") {
                    expectThat(CapabilityMapper.invoke(source = capability))
                        .isA<CapabilityMessage.EnumValue>()
                }
            }
    }
}
