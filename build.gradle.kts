import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import xyz.jpenilla.gremlin.gradle.ShadowGremlin
import xyz.jpenilla.runtask.task.AbstractRun

plugins {
    kotlin("jvm") version "2.3.0"
    id("com.gradleup.shadow") version "9.3.1"
    id("io.github.revxrsal.bukkitkobjects") version "0.0.5"
    id("xyz.jpenilla.gremlin-gradle") version "0.0.9"
    id("co.uzzu.dotenv.gradle") version "4.0.0"
    java
    id("xyz.jpenilla.run-paper") version "3.0.2"
    `maven-publish`
}

bukkitKObjects {
    classes.add("gg.aquatic.waves.Waves")
}

group = "gg.aquatic.waves"
version = "26.0.20"

tasks {
    runServer {
        minecraftVersion("1.21.11")
    }

    build {
        dependsOn(shadowJar)
    }

    processResources {
        val props = mapOf(
            "version" to project.version
        )
        inputs.properties(props)
        filesMatching("*.yml") {
            expand(props)
        }
    }

    test {
        useJUnitPlatform()
    }

    writeDependencies {
        repos.add("https://repo.papermc.io/repository/maven-public/")
        repos.add("https://repo.maven.apache.org/maven2/")
    }
}

tasks.withType(AbstractRun::class) {
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
    maven("https://jitpack.io")
    maven("https://mvn.lumine.io/repository/maven-public/")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
}

val exposedVersion = "0.61.0"
dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    compileOnly("org.slf4j:slf4j-api:2.0.17")

    implementation("gg.aquatic:KMenu:26.0.1")
    implementation("gg.aquatic.replace:Replace:26.0.2")
    implementation("gg.aquatic:Stacked:26.0.1")
    implementation("gg.aquatic:KRegistry:25.0.2")
    implementation("gg.aquatic:KEvent:1.0.4")
    implementation("gg.aquatic:Pakket:26.1.0")
    implementation("gg.aquatic.execute:Execute:26.0.1")
    implementation("gg.aquatic:Kommand:26.0.2")
    implementation("gg.aquatic:Common:26.0.5")
    implementation("gg.aquatic:Kurrency:26.0.1")
    implementation("gg.aquatic:KLocale:26.0.2")
    implementation("gg.aquatic:KLocale-Paper:26.0.2")
    implementation("gg.aquatic:Blokk:26.0.1")
    implementation("gg.aquatic:TreePAPI:26.0.1")
    implementation("gg.aquatic:snapshotmap:26.0.2")
    implementation("gg.aquatic:Statistik:26.0.1")

    runtimeDownload("com.github.ben-manes.caffeine:caffeine:3.2.3")
    runtimeDownload("org.reflections:reflections:0.10.2")
    compileOnly("net.kyori:adventure-text-minimessage:4.26.1")
    compileOnly("net.kyori:adventure-text-serializer-gson:4.26.1")
    compileOnly("net.kyori:adventure-text-serializer-plain:4.26.1")
    compileOnly("com.github.MilkBowl:VaultAPI:1.7.1")
    compileOnly("com.ticxo.modelengine:ModelEngine:R4.0.9")
    compileOnly("me.clip:placeholderapi:2.11.7")

    // Testing
    testImplementation("io.mockk:mockk:1.14.7")
    testImplementation(kotlin("test"))
    testImplementation("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")

    // DB
    runtimeDownload("org.jetbrains.exposed:exposed-core:$exposedVersion")
    runtimeDownload("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    runtimeDownload("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    runtimeDownload("redis.clients:jedis:7.2.1")
    runtimeDownload("com.zaxxer:HikariCP:7.0.2")

    runtimeDownload("org.jetbrains.kotlin:kotlin-stdlib:2.3.0")
    runtimeDownload("org.jetbrains.kotlin:kotlin-reflect:2.3.0")
    runtimeDownload("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
}

configurations {
    runtimeDownload {
        exclude("org.checkerframework", "checker-qual")
        exclude("com.google.code.gson")
    }
    compileOnly {
        extendsFrom(configurations.runtimeDownload.get())
    }
    testImplementation {
        extendsFrom(configurations.runtimeDownload.get())
    }
}

kotlin {
    jvmToolchain(21)
}

val regularJar = tasks.register<ShadowJar>("regularJar") {
    group = "build"
    configurations = listOf(project.configurations.runtimeClasspath.get())
    from(sourceSets.main.get().output)
    archiveBaseName.set("Waves")
    archiveClassifier.set("")
}

tasks.withType<ShadowJar> {
    dependencies {
        exclude(dependency("org.jetbrains.kotlin:.*:.*"))
        exclude(dependency("org.jetbrains.kotlinx:.*:.*"))
        exclude(dependency("org.jetbrains:annotations:.*"))
        exclude(dependency("com.intellij:annotations:.*"))

        exclude(dependency("net.kyori:adventure-api:.*"))
        exclude(dependency("org.javassist:javassist:.*"))
        exclude(dependency("javax.annotation:javax.annotation-api:.*"))
        exclude(dependency("com.google.code.findbugs:jsr305:.*"))
        exclude(dependency("org.slf4j:.*:.*"))
    }

    mergeServiceFiles()
    filesMatching("META-INF/services/**") {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
    }
}

listOf(tasks.shadowJar, tasks.writeDependencies).forEach { taskProvider ->
    taskProvider.configure {
        // Ensure shadowJar is also a fat jar
        if (this is ShadowJar) {
            from(sourceSets.main.get().output)
            configurations = listOf(project.configurations.runtimeClasspath.get())
            archiveClassifier.set("shaded")
        }

        fun reloc(pkg: String) {
            ShadowGremlin.relocate(this, pkg, "gg.aquatic.waves.dependency.$pkg")
        }

        reloc("kotlinx")
        reloc("org.jetbrains.kotlin")
        reloc("kotlin")
        reloc("org.bstats")
        reloc("com.zaxxer.hikari")
    }
}

val maven_username = if (env.isPresent("MAVEN_USERNAME")) env.fetch("MAVEN_USERNAME") else ""
val maven_password = if (env.isPresent("MAVEN_PASSWORD")) env.fetch("MAVEN_PASSWORD") else ""

publishing {
    repositories {
        maven {
            name = "aquaticRepository"
            url = uri("https://repo.nekroplex.com/releases")

            credentials {
                username = maven_username
                password = maven_password
            }
            authentication {
                create<BasicAuthentication>("basic")
            }
        }
    }
    publications {
        create<MavenPublication>("maven") {
            groupId = "gg.aquatic"
            artifactId = "Waves"
            version = project.version.toString()

            artifact(regularJar)
            artifact(tasks.shadowJar)
        }
    }
}