plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.test.balloon)
}

group = "network.marsys.smarthome.hub.integration.zigbee"
version = libs.versions.smarthome.hub.get()

kotlin {
    jvmToolchain(libs.versions.jvm.toolchain.get().toInt())
}

dependencies {
    implementation(projects.common.annotations)
    implementation(projects.feature.integration.application)
    implementation(projects.feature.integration.domain)
    implementation(projects.feature.entity.domain)
    implementation(projects.infrastructure.mqtt)

    implementation(libs.bundles.kotlin.logging)

    implementation(project.dependencies.platform(libs.koin.bom))
    implementation(libs.koin.core)

    implementation(libs.kotlin.coroutines)
    implementation(libs.kotlin.serialization.json)

    implementation(libs.smarthome.domain)

    testImplementation(libs.kotlin.expect.core)

    testImplementation(libs.test.balloon.core)

    testImplementation(libs.turbine)
}
