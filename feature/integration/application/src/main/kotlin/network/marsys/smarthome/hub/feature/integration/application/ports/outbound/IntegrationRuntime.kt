package network.marsys.smarthome.hub.feature.integration.application.ports.outbound

interface IntegrationRuntime {
    suspend fun start()
    suspend fun stop()
}
