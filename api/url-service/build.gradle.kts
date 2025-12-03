dependencies {
    implementation(project(":domain:short-url"))
    implementation(project(":util:distributed-lock"))
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-web")
}