package network.marsys.smarthome.hub.feature.platform.infrastructure

import network.marsys.smarthome.api.mappers.Mapper
import network.marsys.smarthome.api.models.platform.PlatformEventMessage
import network.marsys.smarthome.hub.feature.platform.application.PlatformEvent

object PlatformEventMapper : Mapper<PlatformEvent, PlatformEventMessage> {
    override fun invoke(source: PlatformEvent): PlatformEventMessage =
        when (source) {
            is PlatformEvent.CapabilityUpdated -> CapabilityUpdatedMapper.invoke(source)
        }
}

private object CapabilityUpdatedMapper : Mapper<PlatformEvent.CapabilityUpdated, PlatformEventMessage.CapabilityUpdated> {
    override fun invoke(source: PlatformEvent.CapabilityUpdated): PlatformEventMessage.CapabilityUpdated =
        PlatformEventMessage.CapabilityUpdated(
            identifier = source.identifier.value,
            capability = CapabilityMapper.invoke(source = source.capability),
            occurredAt = source.occurredAt.toString(),
        )
}
