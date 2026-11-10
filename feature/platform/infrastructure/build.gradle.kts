plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.test.balloon)
}

group = "network.marsys.smarthome.hub.feature.platform.infrastructure"
version = libs.versions.smarthome.hub.get()

kotlin {
    jvmToolchain(libs.versions.jvm.toolchain.get().toInt())
}

dependencies {
    implementation(projects.feature.platform.application)

    implementation(project.dependencies.platform(libs.koin.bom))
    implementation(libs.koin.core)

    implementation(libs.kotlin.coroutines)

    implementation(libs.smarthome.domain)

    testImplementation(libs.kotlin.expect.core)

    testImplementation(libs.test.balloon.core)
}
