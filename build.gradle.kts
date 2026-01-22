plugins {
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
