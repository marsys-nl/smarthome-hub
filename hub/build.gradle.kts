plugins {
    alias(libs.plugins.buildconfig)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.test.balloon)
}

group = "network.marsys.smarthome.hub"
version = libs.versions.smarthome.hub.get()

application {
    mainClass.set("network.marsys.smarthome.hub.ApplicationKt")
}

buildConfig {
    packageName("network.marsys.smarthome.hub")

    buildConfigField("VERSION", "${project.version}")
}

kotlin {
    jvmToolchain(libs.versions.jvm.toolchain.get().toInt())
}

ktor {
    fatJar {
        archiveFileName.set("smarthome-hub-${libs.versions.smarthome.hub.get()}.jar")
    }
}

tasks.withType(Tar::class).all {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.withType(Zip::class).all {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

dependencies {
    implementation(projects.feature.entity.domain)
    implementation(projects.feature.integration.application)
    implementation(projects.feature.integration.domain)
    implementation(projects.feature.integration.infrastructure)

    implementation(libs.bundles.kotlin.logging)

    implementation(project.dependencies.platform(libs.koin.bom))
    implementation(libs.koin.ktor)

    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.server.auth.api.key)
    implementation(libs.ktor.server.auth.jwt)
    implementation(libs.ktor.server.config.yaml)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.server.forwarded.header)
    implementation(libs.ktor.server.netty)

    implementation(libs.smarthome.api)
    implementation(libs.smarthome.domain)

    testImplementation(libs.kotlin.expect.core)

    testImplementation(libs.ktor.client.content.negotiation)
    testImplementation(libs.ktor.server.test.host)

    testImplementation(libs.test.balloon.core)
}
