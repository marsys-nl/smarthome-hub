package network.marsys.smarthome.hub.infrastructure.mqtt.container

import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.images.builder.Transferable

internal class MosquittoContainer : GenericContainer<MosquittoContainer>("eclipse-mosquitto:2.0.15") {
    init {
        withExposedPorts(1883)
        withCopyToContainer(
            Transferable.of(
                """
                listener 1883 0.0.0.0
                allow_anonymous true
                persistence false
                """.trimIndent(),
            ),
            "/mosquitto/config/mosquitto.conf",
        )

        waitingFor(Wait.forListeningPort())
    }

    val uri: String
        get() = "tcp://$host:${getMappedPort(1883)}"
}
