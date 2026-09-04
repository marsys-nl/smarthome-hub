plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.test.balloon)
}

group = "network.marsys.smarthome.hub.feature.integration.infrastructure"
version = libs.versions.smarthome.hub.get()

kotlin {
    jvmToolchain(libs.versions.jvm.toolchain.get().toInt())
}

dependencies {
    implementation(projects.feature.entity.domain)
    implementation(projects.feature.integration.application)
    implementation(projects.feature.integration.domain)

    implementation(libs.kotlin.coroutines)

    implementation(libs.oshi.core)

    implementation(libs.smarthome.domain)

    testImplementation(libs.kotlin.expect.core)

    testImplementation(libs.test.balloon.core)

    testImplementation(libs.turbine)
}
