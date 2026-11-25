package network.marsys.smarthome.hub.infrastructure.mqtt.internal

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import network.marsys.smarthome.hub.infrastructure.mqtt.ConnectionState
import network.marsys.smarthome.hub.infrastructure.mqtt.ManagedMqttClient
import network.marsys.smarthome.hub.infrastructure.mqtt.Message
import org.eclipse.paho.mqttv5.client.IMqttAsyncClient
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions

internal class PahoMqttClient(
    private val client: IMqttAsyncClient,
    private val options: MqttConnectionOptions,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ManagedMqttClient {
    private val job = Job()
    private val scope = CoroutineScope(job + dispatcher)
    private val mutex = Mutex()

    private var closed = false
    private val lifecycle = ClientLifecycle()

    private val demander = ConnectionDemander()
    private val connection = PahoConnection(
        client = client,
        options = options,
    )

    private val connectionManager = ConnectionManager(
        connection = connection,
        demander = demander,
    )

    private val subscriptionRegistry = SubscriptionRegistry()
    private val subscriptionExecutor = PahoSubscriptionExecutor(client = client)
    private val subscriptionManager = SubscriptionManager(
        registry = subscriptionRegistry,
        executor = subscriptionExecutor,
    )

    private val publishExecutor = PahoPublishExecutor(client = client)
    private val publisher = Publisher(
        executor = publishExecutor,
        demander = demander,
        state = connectionManager.state,
    )

    override val state: StateFlow<ConnectionState> =
        connectionManager.state

    init {
        client.setCallback(
            PahoCallback(
                onConnectionChanged = connection::updateConnectionState,
                onMessageReceived = subscriptionRegistry::dispatch,
            ),
        )

        job.invokeOnCompletion { cause ->
            terminate(
                cause = ClientClosedException(cause = cause),
            )
        }

        scope.launch {
            connectionManager.run(
                block = subscriptionManager::run,
            )
        }
    }

    override suspend fun close() {
        withContext(NonCancellable) {
            mutex.withLock {
                if (closed) {
                    return@withLock
                }

                terminate(cause = ClientClosedException())

                job.cancelAndJoin()

                withContext(dispatcher) {
                    client.close(true)
                }

                connectionManager.onClosed()
                closed = true
            }
        }
    }

    override suspend fun publish(topic: String, payload: ByteArray) {
        lifecycle.execute {
            publisher.publish(
                topic = TopicName(topic),
                payload = payload,
            )
        }
    }

    override fun subscribe(vararg filters: String): Flow<Message> {
        val subscription = subscriptionRegistry.subscribe(
            filters = filters
                .map(::TopicFilter)
                .toSet(),
        )

        return flow {
            lifecycle.ensureOpen()

            demander.demand {
                emitAll(subscription)
            }
        }
    }

    private fun terminate(cause: ClientClosedException) {
        lifecycle.close(cause)
        subscriptionRegistry.close(cause)
    }
}
