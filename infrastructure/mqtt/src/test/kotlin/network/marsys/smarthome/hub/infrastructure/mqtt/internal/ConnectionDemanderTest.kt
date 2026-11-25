package network.marsys.smarthome.hub.infrastructure.mqtt.internal

import de.infix.testBalloon.framework.core.testSuite
import dev.nmarsman.expect.api.expectThat
import dev.nmarsman.expect.api.expectThrows
import dev.nmarsman.expect.assertions.containsExactly
import dev.nmarsman.expect.assertions.isEqualTo
import dev.nmarsman.expect.assertions.isFalse
import dev.nmarsman.expect.assertions.isTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
val ConnectionDemanderTest by testSuite(
    name = "Connection demander tests",
) {
    test(name = "Connection is initially not required") {
        val demander = ConnectionDemander()

        expectThat(demander.required.first())
            .isFalse()
    }

    test(name = "Connection is required during active usage") {
        val demander = ConnectionDemander()

        demander.demand {
            expectThat(demander.required.first())
                .isTrue()
        }

        expectThat(demander.required.first())
            .isFalse()
    }

    test(name = "Connection remains required until last consumer completes") {
        runTest {
            val demander = ConnectionDemander()

            val first = launch {
                demander.demand {
                    awaitCancellation()
                }
            }

            val second = launch {
                demander.demand {
                    awaitCancellation()
                }
            }

            runCurrent()

            expectThat(demander.required.first())
                .isTrue()

            first.cancelAndJoin()

            expectThat(demander.required.first())
                .isTrue()

            second.cancelAndJoin()

            expectThat(demander.required.first())
                .isFalse()
        }
    }

    test(name = "Demand is released when consumer fails") {
        val demander = ConnectionDemander()

        expectThrows<IllegalStateException> {
            demander.demand {
                throw IllegalStateException("Consumer failed")
            }
        }

        expectThat(demander.required.first())
            .isFalse()
    }

    test(name = "Demand returns result of consumer block") {
        val demander = ConnectionDemander()

        val result = demander.demand {
            "Hello, world!"
        }

        expectThat(result)
            .isEqualTo("Hello, world!")
    }

    test(name = "Required only emits when connection demand changes") {
        runTest {
            val demander = ConnectionDemander()
            val emissions = mutableListOf<Boolean>()

            backgroundScope.launch {
                demander.required.collect {
                    emissions.add(it)
                }
            }

            runCurrent()

            val first = launch {
                demander.demand {
                    awaitCancellation()
                }
            }

            runCurrent()

            val second = launch {
                demander.demand {
                    awaitCancellation()
                }
            }

            runCurrent()

            first.cancelAndJoin()
            runCurrent()

            second.cancelAndJoin()
            runCurrent()

            expectThat(emissions)
                .containsExactly(false, true, false)
        }
    }
}
