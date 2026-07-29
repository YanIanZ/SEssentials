plugins {
    java
    id("com.gradleup.shadow") version "9.6.1"
}

group = "dev.iyanz"
version = "1.0.0"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.extendedclip.com/releases/")
    maven("https://jitpack.io")
}

dependencies {
    // Paper API 1.21.9 — provides the Folia threaded-regions scheduler + Brigadier.
    compileOnly("io.papermc.paper:paper-api:1.21.9-R0.1-SNAPSHOT")

    // Soft integrations (provided by the server when present).
    compileOnly("com.github.MilkBowl:VaultAPI:1.7.1")
    compileOnly("me.clip:placeholderapi:2.11.6")

    testImplementation("org.junit.jupiter:junit-jupiter:5.11.3")
    testImplementation("io.papermc.paper:paper-api:1.21.9-R0.1-SNAPSHOT")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

tasks.shadowJar {
    archiveClassifier.set("")
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.compilerArgs.add("-parameters")
}
