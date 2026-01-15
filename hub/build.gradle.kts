plugins {
    alias(libs.plugins.kotlin.jvm)
}

group = "network.marsys.smarthome.hub"
version = libs.versions.smarthome.hub.get()

kotlin {
    jvmToolchain(libs.versions.jvm.toolchain.get().toInt())
}

dependencies {
    //
}
