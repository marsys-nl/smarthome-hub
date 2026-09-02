package network.marsys.smarthome.hub.feature.entity.domain.capability

data class OnOff(
    override val current: Boolean,
    override val context: Context = Context.Empty,
) : Capability<Boolean>() {
    override fun create(
        current: Boolean,
        context: Context,
    ): OnOff = copy(
        current = current,
        context = context,
    )
}
