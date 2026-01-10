plugins {
    kotlin("jvm") version "2.3.0"
    id("com.gradleup.shadow") version "9.3.1"
    id("io.github.revxrsal.bukkitkobjects") version "0.0.5"
    id("xyz.jpenilla.gremlin-gradle") version "0.0.9"
    id("co.uzzu.dotenv.gradle") version "4.0.0"
    java
    id("xyz.jpenilla.run-paper") version "3.0.2"
}

bukkitKObjects {
    classes.add("gg.aquatic.waves.Waves")
}

group = "gg.aquatic.waves"
version = "26.0.1"

tasks {
    runServer {
        minecraftVersion("1.21.11")
    }

    build {
        dependsOn(shadowJar)
    }
}

tasks.withType(xyz.jpenilla.runtask.task.AbstractRun::class) {
    javaLauncher = javaToolchains.launcherFor {
        vendor = JvmVendorSpec.JETBRAINS
        languageVersion = JavaLanguageVersion.of(21)
    }
    jvmArgs("-XX:+AllowEnhancedClassRedefinition")
}


repositories {
    mavenCentral()
    maven {
        name = "papermc"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
    maven {
        name = "aquatic-releases"
        url = uri("https://repo.nekroplex.com/releases")
    }
    maven("https://libraries.minecraft.net")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    compileOnly("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")

    implementation("gg.aquatic:KMenu:26.0.1")
    implementation("gg.aquatic.replace:Replace:26.0.2")
    implementation("gg.aquatic:Stacked:26.0.1")
    implementation("gg.aquatic:KRegistry:25.0.1")
    implementation("gg.aquatic:KEvent:1.0.4")
    implementation("gg.aquatic:Pakket:26.1.0")
    implementation("gg.aquatic.execute:Execute:26.0.1")
    implementation("gg.aquatic:Kommand:26.0.2")

    implementation("org.reflections:reflections:0.10.2")
    implementation("net.kyori:adventure-text-minimessage:4.26.1")
    compileOnly("net.kyori:adventure-text-serializer-gson:4.26.1")
    compileOnly("net.kyori:adventure-text-serializer-plain:4.26.1")

    // Testing
    testImplementation("io.mockk:mockk:1.14.7")
    testImplementation(kotlin("test"))
    testImplementation("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
}

kotlin {
    jvmToolchain(21)
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    archiveFileName.set("Waves-${project.version}.jar")
    archiveClassifier.set("")

    dependencies {
        exclude(dependency("org.jetbrains.kotlin:.*:.*"))
        exclude(dependency("org.jetbrains.kotlinx:.*:.*"))
        exclude(dependency("org.jetbrains:annotations:.*"))
    }

    relocate("kotlinx", "gg.aquatic.waves.libs.kotlinx")
    relocate("org.jetbrains.kotlin", "gg.aquatic.waves.libs.kotlin")
    relocate("kotlin", "gg.aquatic.waves.libs.kotlin")
    relocate("org.bstats", "gg.aquatic.waves.shadow.bstats")
    //relocate("com.undefined", "gg.aquatic.waves.shadow.undefined")

    relocate("com.zaxxer.hikari", "gg.aquatic.waves.libs.hikari")
}