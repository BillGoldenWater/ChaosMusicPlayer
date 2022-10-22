plugins {
    kotlin("jvm") version "1.7.20"
}

group = "indi.goldenwater.chaosmusicplayer.common"
version = "1.1.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))

    implementation("com.github.wendykierp:JTransforms:3.1")
}