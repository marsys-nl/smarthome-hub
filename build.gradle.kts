plugins {
    alias(libs.plugins.detekt) apply true
    alias(libs.plugins.kotlin.jvm) apply false
}

val ktlint: Configuration by configurations.creating

dependencies {
    ktlint(libs.ktlint) {
        attributes {
            attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling.EXTERNAL))
        }
    }
}

detekt {
    allRules = true
    buildUponDefaultConfig = true
    config.from("$rootDir/config/detekt/detekt.yml")

    source.from(
        "$rootDir/hub/src/main/kotlin",
    )
}

tasks.register("ktlintCheck", JavaExec::class) {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Check Kotlin code style"
    classpath = ktlint
    mainClass.set("com.pinterest.ktlint.Main")
    args(
        "**.kt",
        "**.kts",
        "!**/build/**",
    )
}

tasks.withType<dev.detekt.gradle.Detekt>().configureEach {
    reports {
        html.required.set(true)

        checkstyle.required.set(false)
        markdown.required.set(false)
        sarif.required.set(false)
    }
}
