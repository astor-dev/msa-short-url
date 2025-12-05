dependencies {
    implementation(project(":outbox"))
    implementation("org.springframework.cloud:spring-cloud-stream")
    implementation("org.springframework.cloud:spring-cloud-stream-binder-kafka")
    testImplementation("org.springframework.cloud:spring-cloud-stream-test-binder")
}