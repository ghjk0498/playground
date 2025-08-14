plugins {
    kotlin("jvm") version "1.9.25"
    kotlin("plugin.spring") version "1.9.25"
    id("org.springframework.boot") version "3.5.4"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "khg.example"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
    // Swagger/OpenAPI for Spring WebFlux
    implementation("org.apache.commons:commons-lang3:3.18.0")
    implementation("org.springdoc:springdoc-openapi-starter-webflux-ui:2.8.9")
    // Lettuce Redis client (direct, per-call connect/close)
    implementation("io.lettuce:lettuce-core:6.5.3.RELEASE")
    // R2DBC (reactive Postgres)
    implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")
    implementation("org.postgresql:r2dbc-postgresql:1.0.7.RELEASE")
    // Flyway (JDBC) for schema migrations
    implementation("org.postgresql:postgresql:42.7.7")
    implementation("org.flywaydb:flyway-core:11.10.5")
    runtimeOnly("org.flywaydb:flyway-database-postgresql:11.10.5")

    developmentOnly("org.springframework.boot:spring-boot-devtools")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.projectreactor:reactor-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
