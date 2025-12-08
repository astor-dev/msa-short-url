plugins {
    kotlin("jvm") version "2.2.10"
    kotlin("plugin.serialization") version "2.2.10"
    application
}

group = "com.naver.pay"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    // --- HTTP Client ---
    implementation("io.ktor:ktor-client-core:2.3.5")
    implementation("io.ktor:ktor-client-cio:2.3.5")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.5")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.5")
    
    // --- Kotlinx Serialization ---
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    
    // --- Logging ---
    implementation("io.github.oshai:kotlin-logging-jvm:7.0.13")
    implementation("ch.qos.logback:logback-classic")

    // --- Object Mapper ---
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    
    // --- Testing ---
    testImplementation(kotlin("test"))
}

application {
    mainClass.set("com.naver.pay.traffic.TrafficGeneratorKt")
}

tasks.named<JavaExec>("run") {
    standardInput = System.`in`
    systemProperty("org.gradle.console", "plain")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}