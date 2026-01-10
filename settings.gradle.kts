plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
rootProject.name = "Waves"

includeBuild("KEvent")
includeBuild("KRegistry")
includeBuild("Execute")
includeBuild("Kommand")
includeBuild("Pakket")
includeBuild("Replace")
includeBuild("Stacked")
includeBuild("KMenu")