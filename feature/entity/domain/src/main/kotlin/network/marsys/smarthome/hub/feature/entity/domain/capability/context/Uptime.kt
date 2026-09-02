package network.marsys.smarthome.hub.feature.entity.domain.capability.context

import network.marsys.smarthome.hub.feature.entity.domain.capability.Capability

data object Application : Capability.Context.Element {
    override val key: Capability.Context.Key<Application>
        get() = Key

    data object Key : Capability.Context.Key<Application>
}

data object Host : Capability.Context.Element {
    override val key: Capability.Context.Key<Host>
        get() = Key

    data object Key : Capability.Context.Key<Host>
}
