plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.test.balloon)
}

group = "network.marsys.smarthome.hub.infrastructure.mqtt"
version = libs.versions.smarthome.hub.get()

val integrationTest = sourceSets.create("integrationTest") {
    kotlin.srcDir("src/integration/kotlin")
    resources.srcDir("src/integration/resources")

    compileClasspath += sourceSets.main.get().output
    runtimeClasspath += sourceSets.main.get().output
}

configurations {
    named(integrationTest.implementationConfigurationName) {
        extendsFrom(testImplementation.get())
    }

    named(integrationTest.runtimeOnlyConfigurationName) {
        extendsFrom(testRuntimeOnly.get())
    }
}

kotlin {
    jvmToolchain(libs.versions.jvm.toolchain.get().toInt())
}

dependencies {
    compileOnly(projects.common.annotations)

    implementation(libs.bundles.kotlin.logging)

    implementation(libs.eclipse.paho.mqtt.client)

    implementation(libs.kotlin.coroutines)

    testImplementation(libs.kotlin.expect.core)

    testImplementation(libs.test.balloon.core)
    testImplementation(libs.test.containers)
}

tasks.register<Test>("integrationTest") {
    description = "Runs the MQTT integration tests."
    group = "verification"

    testClassesDirs = integrationTest.output.classesDirs
    classpath = integrationTest.runtimeClasspath

    useJUnitPlatform()

    shouldRunAfter(tasks.test)
}
