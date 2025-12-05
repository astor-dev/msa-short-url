dependencies {
    implementation(project(":short-url"))
    implementation(project(":short-url-stats"))
    implementation(project(":domain:resolved-short-url"))
    implementation("org.springframework.cloud:spring-cloud-stream")
    implementation("org.springframework.cloud:spring-cloud-stream-binder-kafka")
    implementation("nl.basjes.parse.useragent:yauaa:7.32.0")
    testImplementation("org.springframework.cloud:spring-cloud-stream-test-binder")
}