dependencies {
    implementation(project(":domain:short-url"))
    implementation(project(":domain:short-url-stats"))
    implementation(project(":domain:resolved-short-url"))
    implementation("org.springframework.cloud:spring-cloud-stream")
    implementation("org.springframework.cloud:spring-cloud-stream-binder-kafka")
    testImplementation("org.springframework.cloud:spring-cloud-stream-test-binder")
}