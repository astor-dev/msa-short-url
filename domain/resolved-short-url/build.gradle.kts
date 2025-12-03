dependencies {
    implementation(project(":util:distributed-lock"))
    implementation(project(":domain:short-url"))
    implementation("org.springframework:spring-context")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
}