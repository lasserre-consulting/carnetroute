import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    kotlin("jvm") version "2.3.20"
    kotlin("plugin.serialization") version "2.3.20"
    id("com.gradleup.shadow") version "8.3.6"
    application
}

group = "com.carnetroute"
version = "2.0.0"

kotlin {
    jvmToolchain(25)
}

application {
    mainClass.set("com.carnetroute.ApplicationKt")
}

val ktorVersion = "3.4.2"
val exposedVersion = "0.61.0"
val kotestVersion = "5.9.1"
val testcontainersVersion = "1.21.4"

repositories {
    mavenCentral()
}

dependencies {
    // Ktor server
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-server-auth:$ktorVersion")
    implementation("io.ktor:ktor-server-auth-jwt:$ktorVersion")
    implementation("io.ktor:ktor-server-cors:$ktorVersion")
    implementation("io.ktor:ktor-server-default-headers:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages:$ktorVersion")
    implementation("io.ktor:ktor-server-call-logging:$ktorVersion")
    implementation("io.ktor:ktor-server-metrics-micrometer:$ktorVersion")
    implementation("io.ktor:ktor-server-websockets:$ktorVersion")

    // Ktor client
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-client-logging:$ktorVersion")

    // Ktor serialization
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")

    // Kotlinx
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.1")

    // Exposed ORM
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-kotlin-datetime:$exposedVersion")

    // Database
    implementation("org.postgresql:postgresql:42.7.10")
    implementation("com.zaxxer:HikariCP:6.3.3")

    // Flyway migrations
    implementation("org.flywaydb:flyway-core:12.4.0")
    implementation("org.flywaydb:flyway-database-postgresql:12.4.0")

    // Redis
    implementation("redis.clients:jedis:6.2.0")

    // NATS messaging
    implementation("io.nats:jnats:2.25.2")

    // JWT
    implementation("com.auth0:java-jwt:4.5.1")

    // BCrypt
    implementation("at.favre.lib:bcrypt:0.10.2")

    // Micrometer / Prometheus
    implementation("io.micrometer:micrometer-registry-prometheus:1.16.5")

    // Koin DI
    implementation("io.insert-koin:koin-ktor:4.2.1")
    implementation("io.insert-koin:koin-core:4.2.1")

    // Logging
    implementation("ch.qos.logback:logback-classic:1.5.32")

    // Tests
    testImplementation("io.kotest:kotest-runner-junit5:$kotestVersion")
    testImplementation("io.kotest:kotest-assertions-core:$kotestVersion")
    testImplementation("io.mockk:mockk:1.14.9")
    testImplementation("org.testcontainers:testcontainers:$testcontainersVersion")
    testImplementation("org.testcontainers:postgresql:$testcontainersVersion")
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<ShadowJar> {
    archiveBaseName.set("carnetroute-backend")
    archiveClassifier.set("all")
    archiveVersion.set(version.toString())
    mergeServiceFiles()
}

tasks.assemble {
    dependsOn(tasks.withType<ShadowJar>())
}
