plugins {
    java
    id("org.springframework.boot") version "3.2.0"
    id("io.spring.dependency-management") version "1.1.4"
    id("io.freefair.lombok") version "8.4"
}

group = "ru.collegehub"
version = "0.0.1-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_17
}

val testcontainersVersion = "1.17.6"
val postgresVersion = "42.5.4"
val liquidBaseVersion = "4.25.0"
val modelMapperVersion = "3.1.1"

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.kafka:spring-kafka")
    implementation("org.liquibase:liquibase-core:${liquidBaseVersion}")
    implementation("io.jsonwebtoken:jjwt:0.9.1")
    implementation("org.modelmapper:modelmapper:${modelMapperVersion}")
    runtimeOnly("org.postgresql:postgresql:${postgresVersion}")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:postgresql:${testcontainersVersion}")
    testImplementation("org.testcontainers:junit-jupiter:${testcontainersVersion}")
}

tasks.withType<Test> {
    useJUnitPlatform()
}