package network.marsys.smarthome.hub.feature.entity.domain.capability

data class OnOff(
    override val current: Boolean,
) : Capability<Boolean> {
    override fun updateWith(value: Boolean): Capability<Boolean> =
        copy(current = value)
}
