package network.marsys.smarthome.hub.feature.entity.domain.capability

data class OnOff(
    override val current: Boolean,
) : Capability<Boolean>
