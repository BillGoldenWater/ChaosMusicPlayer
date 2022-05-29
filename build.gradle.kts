import java.net.URI

plugins {
    kotlin("jvm") version "1.6.21"
}

group = "indi.goldenwater"
version = "1.0.0"

repositories {
    mavenCentral()
    maven {
        name = "papermc-repo"
        url = URI("https://papermc.io/repo/repository/maven-public/")
    }
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.6.21")

    compileOnly("io.papermc.paper:paper-api:1.18.2-R0.1-SNAPSHOT")
}

tasks {
    compileKotlin {
        kotlinOptions {
            jvmTarget = "17"
        }
    }

    val fatJar = register<Jar>("fatJar") {
        dependsOn.addAll(
            listOf(
                "compileJava",
                "compileKotlin",
                "processResources"
            )
        )
        archiveClassifier.set("standalone") // Naming the jar
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE

        val sourcesMain = sourceSets.main.get()
        val contents = configurations.runtimeClasspath.get()
            .map { if (it.isDirectory) it else zipTree(it) } +
                sourcesMain.output
        from(contents)
    }
    build {
        dependsOn(fatJar) // Trigger fat jar creation during build
    }

    processResources {
        val props = mutableMapOf<String, String>()
        props["version"] = version as String

        filesMatching("plugin.yml") {
            expand(props)
        }
    }
}