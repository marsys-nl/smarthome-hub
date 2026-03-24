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
                includeModule("network.marsys.smarthome", "smarthome-api")
                includeModule("network.marsys.smarthome", "smarthome-api-jvm")
            }
        }
    }
}

include(":hub")
