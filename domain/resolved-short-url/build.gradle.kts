dependencies {
    implementation(project(":util:distributed-lock"))
    implementation(project(":short-url"))
    implementation(project(":domain:short-url-stats"))
    implementation("org.springframework:spring-context")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
}