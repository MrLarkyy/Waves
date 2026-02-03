plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
rootProject.name = "Waves"

val submodules = listOf(
    "KMenu" to "gg.aquatic:KMenu",
    "KRegistry" to "gg.aquatic:KRegistry",
    "KEvent" to "gg.aquatic:KEvent",
    "Pakket" to "gg.aquatic:Pakket",
    "Stacked" to "gg.aquatic:Stacked",
    "Execute" to "gg.aquatic.execute:Execute",
    "Replace" to "gg.aquatic.replace:Replace",
    "Kommand" to "gg.aquatic:Kommand",
    "KLocale" to "gg.aquatic:KLocale",
    "Kurrency" to "gg.aquatic:Kurrency",
    "Blokk" to "gg.aquatic:Blokk",
    "TreePAPI" to "gg.aquatic:TreePAPI",
    "SnapshotMap" to "gg.aquatic:snapshotmap",
    "AquaticCommon" to "gg.aquatic:Common",
    "Statistik" to "gg.aquatic:Statistik",
    "KHolograms" to "gg.aquatic:KHolograms",
    "Clientside" to "gg.aquatic:Clientside",
    "Dispatch" to "gg.aquatic:Dispatch"
)

submodules.forEach { (folder, artifact) ->
    includeBuild(folder) {
        dependencySubstitution {
            when (folder) {
                "KHolograms" -> {
                    substitute(module("gg.aquatic:KHolograms")).using(project(":core"))
                    substitute(module("gg.aquatic:KHolograms-serialization")).using(project(":serialization"))
                }
                "KMenu" -> {
                    substitute(module("gg.aquatic:KMenu")).using(project(":core"))
                    substitute(module("gg.aquatic:KMenu-serialization")).using(project(":serialization"))
                }
                else -> {
                    substitute(module(artifact)).using(project(":"))
                }
            }
            if (folder == "KLocale") {
                substitute(module("gg.aquatic:KLocale-Paper")).using(project(":Paper"))
                substitute(module("gg.aquatic:KLocale")).using(project(":Common"))
            }
        }
    }
}
