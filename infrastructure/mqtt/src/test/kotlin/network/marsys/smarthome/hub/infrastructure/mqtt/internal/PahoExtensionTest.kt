package network.marsys.smarthome.hub.infrastructure.mqtt.internal

import de.infix.testBalloon.framework.core.testSuite
import dev.nmarsman.expect.api.expectThat
import dev.nmarsman.expect.api.expectThrows
import dev.nmarsman.expect.assertions.isEqualTo
import dev.nmarsman.expect.assertions.isFalse
import dev.nmarsman.expect.assertions.isTrue
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.eclipse.paho.mqttv5.client.MqttActionListener
import org.eclipse.paho.mqttv5.client.MqttToken

@OptIn(ExperimentalCoroutinesApi::class)
val PahoExtensionTest by testSuite(
    name = "Paho extension methods tests",
) {
    test(name = "Await returns token when operation succeeds") {
        val token = MqttToken()

        val result = await { listener ->
            listener.onSuccess(token)
        }

        expectThat(result)
            .isEqualTo(token)
    }

    test(name = "Await throws exception when operation fails") {
        expectThrows<IllegalStateException> {
            await {
                throw IllegalStateException("Operation failed")
            }
        }
    }

    test(name = "Await suspends until callback is invoked") {
        runTest {
            val token = MqttToken()
            lateinit var callback: MqttActionListener

            val result = async {
                await { listener ->
                    callback = listener
                }
            }

            runCurrent()

            expectThat(result)
                .get(Deferred<*>::isCompleted)
                .isFalse()

            callback.onSuccess(token)

            expectThat(result.await())
                .isEqualTo(token)
        }
    }

    test(name = "Can be cancelled while awaiting completion") {
        runTest {
            val job = launch {
                await {
                    // No-op
                }
            }

            runCurrent()

            expectThat(job)
                .with(Job::isCancelled) { isFalse() }
                .with(Job::isActive) { isTrue() }

            job.cancelAndJoin()

            expectThat(job)
                .with(Job::isCancelled) { isTrue() }
                .with(Job::isActive) { isFalse() }
        }
    }

    test(name = "Handles callback after cancellation") {
        runTest {
            val token = MqttToken()
            lateinit var callback: MqttActionListener

            val job = launch {
                await { listener ->
                    callback = listener
                }
            }

            runCurrent()

            job.cancelAndJoin()

            callback.onSuccess(token)

            expectThat(job)
                .with(Job::isCancelled) { isTrue() }
                .with(Job::isActive) { isFalse() }
        }
    }
}
