package network.marsys.smarthome.hub.feature.entity.domain.entity

import de.infix.testBalloon.framework.core.testSuite
import dev.nmarsman.expect.api.expectThat
import dev.nmarsman.expect.assertions.isEqualTo
import dev.nmarsman.expect.assertions.isNull
import network.marsys.smarthome.domain.i18n.TranslationKey

val EntityNamingTest by testSuite(
    name = "Entity naming test",
) {
    test("Entity naming should use the default key when no key is provided") {
        val naming = System.naming()

        expectThat(naming.key)
            .isEqualTo(System.defaultNamingKey)

        expectThat(naming.qualifier)
            .isNull()
    }

    test("Entity naming should use the provided key when a key is provided") {
        val naming = System.naming(
            key = TranslationKey("translation-key"),
        )

        expectThat(naming.key)
            .isEqualTo(TranslationKey("translation-key"))

        expectThat(naming.qualifier)
            .isNull()
    }

    test("Entity naming should use the provided key and qualifier when both are provided") {
        val naming = System.naming(
            key = TranslationKey("translation-key"),
            qualifier = TranslationKey("qualifier"),
        )

        expectThat(naming.key)
            .isEqualTo(TranslationKey("translation-key"))

        expectThat(naming.qualifier)
            .isEqualTo(TranslationKey("qualifier"))
    }
}
