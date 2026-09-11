import java.net.URI

rootProject.name = "smarthome-hub"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositories {
        google()
        mavenCentral()

        maven {
            name = "Central Portal Snapshots"
            url = URI.create("https://central.sonatype.com/repository/maven-snapshots/")

            mavenContent {
                snapshotsOnly()
            }

            content {
                includeModuleByRegex("dev\\.nmarsman\\.expect", "kotlin-expect-core.*")
                includeModuleByRegex("network\\.marsys\\.smarthome", "smarthome-.*")
            }
        }
    }
}

include(
    ":core:eventstore:application",
    ":core:eventstore:infrastructure",
    ":feature:entity:application",
    ":feature:entity:domain",
    ":feature:integration:application",
    ":feature:integration:domain",
    ":feature:integration:infrastructure",
    ":hub",
)
