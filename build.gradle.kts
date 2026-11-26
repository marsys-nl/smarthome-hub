import kotlinx.kover.gradle.plugin.dsl.AggregationType
import kotlinx.kover.gradle.plugin.dsl.CoverageUnit
import kotlinx.kover.gradle.plugin.dsl.GroupingEntityType

plugins {
    alias(libs.plugins.buildconfig) apply false
    alias(libs.plugins.detekt) apply true
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.kover) apply true
    alias(libs.plugins.ktor) apply false
    alias(libs.plugins.test.balloon) apply false
}

val ktlint = configurations.create("ktlint")

dependencies {
    ktlint(libs.ktlint) {
        attributes {
            attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling.EXTERNAL))
        }
    }
}

detekt {
    allRules = true
    buildUponDefaultConfig = true
    config.from("$rootDir/config/detekt/detekt.yml")

    source.from(
        "$rootDir/hub/src/main/kotlin",
    )
}

kover {
    merge {
        allProjects {
            it.buildFile.exists()
        }

        instrumentation {
            disabledForTestTasks.add("integrationTest")
        }

        sources {
            excludedSourceSets.add("integrationTest")
        }
    }

    reports {
        filters {
            excludes {
                // Exclude code annotated by @ExcludeFromCoverage
                annotatedBy("*.ExcludeFromCoverage")

                // Exclude main entry point — tested indirectly via integration tests
                classes("network.marsys.smarthome.hub.ApplicationKt*")

                // Exclude fake implementations that are temporary.
                classes("network.marsys.smarthome.hub.feature.integration.infrastructure.FakeIntegrationAdapter*")

                // Don't include dependency injection code in coverage reports
                classes(
                    "*.di.*",
                    $$$"*$$inlined$inject$default*",
                )
            }
        }

        verify {
            rule {
                groupBy.set(GroupingEntityType.CLASS)

                minBound(
                    minValue = 100,
                    coverageUnits = CoverageUnit.BRANCH,
                    aggregationForGroup = AggregationType.COVERED_PERCENTAGE,
                )
            }
        }
    }
}

tasks.register("ktlintCheck", JavaExec::class) {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Check Kotlin code style"
    classpath = ktlint
    mainClass.set("com.pinterest.ktlint.Main")
    args(
        "**.kt",
        "**.kts",
        "!**/build/**",
    )
}

tasks.withType<dev.detekt.gradle.Detekt>().configureEach {
    reports {
        html.required.set(true)

        checkstyle.required.set(false)
        markdown.required.set(false)
        sarif.required.set(false)
    }
}
