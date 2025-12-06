apply(plugin = "org.jetbrains.kotlin.plugin.spring")

dependencies {
    implementation(project(":util:distributed-lock"))
    implementation("org.springframework.boot:spring-boot-starter-data-mongodb")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
}