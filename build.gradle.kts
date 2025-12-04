import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("java")
    id("java-library")
    id("org.springframework.boot") version "3.5.8" apply false
    kotlin("jvm") version "2.2.10" apply false
    kotlin("plugin.spring") version "2.2.10" apply false
    kotlin("plugin.jpa") version "2.2.10" apply false
}

group = "com.naver.pay"
version = "1.0-SNAPSHOT"

subprojects {
    apply(plugin = "java")
    apply(plugin = "idea")
    apply(plugin = "org.jetbrains.kotlin.jvm")


    repositories {
        mavenCentral()
    }

    configure<JavaPluginExtension> {
        toolchain {
            languageVersion = JavaLanguageVersion.of(21)
        }
    }

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        compilerOptions {
            freeCompilerArgs.add("-Xjsr305=strict")
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }

    dependencies {
        // --- Spring Boot BOM ---
        implementation(platform("org.springframework.boot:spring-boot-dependencies:3.5.8"))
        annotationProcessor(platform("org.springframework.boot:spring-boot-dependencies:3.5.8"))
        implementation(platform("org.springframework.cloud:spring-cloud-dependencies:2025.0.0"))
        annotationProcessor(platform("org.springframework.cloud:spring-cloud-dependencies:2025.0.0"))

        // --- Kotlin Core ---
        implementation("org.jetbrains.kotlin:kotlin-stdlib")
        implementation("org.jetbrains.kotlin:kotlin-reflect")
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")


        // --- Object Mapper ---
        implementation("com.fasterxml.jackson.core:jackson-databind")
        implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
        implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

        // --- Logging ---
        implementation("io.github.oshai:kotlin-logging-jvm:7.0.13")

        // --- Testing ---
        testImplementation("io.mockk:mockk:1.14.6")
        testImplementation(platform("org.testcontainers:testcontainers-bom:1.21.3"))
        implementation(platform("io.kotest:kotest-bom:6.0.5"))
        testImplementation("io.kotest:kotest-runner-junit5-jvm")
        testImplementation("io.kotest:kotest-assertions-core-jvm")
        testImplementation("io.kotest:kotest-property-jvm")
        testRuntimeOnly("org.junit.platform:junit-platform-launcher")
        testImplementation("org.testcontainers:junit-jupiter")
    }

    tasks.test {
        useJUnitPlatform()
    }
}