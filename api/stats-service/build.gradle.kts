dependencies {
    implementation(project(":domain:short-url-stats"))
    implementation(project(":domain:resolved-short-url"))
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-web")
}