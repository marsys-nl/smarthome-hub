plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktor)
}

group = "network.marsys.smarthome.hub"
version = libs.versions.smarthome.hub.get()

application {
    mainClass.set("network.marsys.smarthome.hub.ApplicationKt")
}

kotlin {
    jvmToolchain(libs.versions.jvm.toolchain.get().toInt())
}

ktor {
    fatJar {
        archiveFileName.set("smarthome-hub-${libs.versions.smarthome.hub.get()}.jar")
    }
}

dependencies {
    implementation(libs.bundles.kotlin.logging)

    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.server.auth.api.key)
    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.server.config.yaml)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.server.forwarded.header)
    implementation(libs.ktor.server.netty)

    implementation(libs.smarthome.api)
}
