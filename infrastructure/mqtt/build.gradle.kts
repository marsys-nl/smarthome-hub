plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.test.balloon)
}

group = "network.marsys.smarthome.hub.infrastructure.mqtt"
version = libs.versions.smarthome.hub.get()

kotlin {
    jvmToolchain(libs.versions.jvm.toolchain.get().toInt())

    sourceSets {
        test {
            kotlin.srcDir("src/integration/kotlin")
        }
    }
}

dependencies {
    implementation(libs.bundles.kotlin.logging)

    implementation(libs.eclipse.paho.mqtt.client)

    implementation(libs.kotlin.coroutines)

    testImplementation(libs.kotlin.expect.core)

    testImplementation(libs.test.balloon.core)
    testImplementation(libs.test.containers)
}
