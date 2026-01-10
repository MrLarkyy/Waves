plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
rootProject.name = "Waves"

include("Replace")
include("KMenu")
include("Stacked")
include("KRegistry")
include("Pakket")
include("Execute")
include("Kommand")
include("KEvent")

include("Pakket:API")
include("Pakket:NMS_1_21_9")