apply(plugin = "org.jetbrains.kotlin.plugin.jpa")
apply(plugin = "java-library")

dependencies {
    api("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    runtimeOnly("com.mysql:mysql-connector-j")

    implementation("org.springframework.cloud:spring-cloud-stream")
    implementation("org.springframework.cloud:spring-cloud-stream-binder-kafka")
    testImplementation("org.springframework.cloud:spring-cloud-stream-test-binder")

    testImplementation("org.testcontainers:mysql")
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.junit.vintage")
        exclude(group = "org.mockito")
    }
    testImplementation("io.kotest:kotest-extensions-spring")
    testImplementation("com.ninja-squad:springmockk:4.0.2")
}