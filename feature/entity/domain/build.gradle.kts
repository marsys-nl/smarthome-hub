plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.test.balloon)
}

group = "network.marsys.smarthome.hub.feature.entity.domain"
version = libs.versions.smarthome.hub.get()

kotlin {
    jvmToolchain(libs.versions.jvm.toolchain.get().toInt())
}

dependencies {
    implementation(libs.smarthome.domain)

    implementation(libs.kotlin.coroutines)

    testImplementation(libs.kotlin.expect.core)

    testImplementation(libs.test.balloon.core)
}
