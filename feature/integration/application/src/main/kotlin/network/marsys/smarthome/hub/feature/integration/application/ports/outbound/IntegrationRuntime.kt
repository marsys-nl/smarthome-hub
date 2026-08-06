package network.marsys.smarthome.hub.feature.integration.application.ports.outbound

interface IntegrationRuntime {
    fun start()
    suspend fun stop()
}
