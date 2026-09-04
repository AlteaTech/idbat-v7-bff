plugins {
    kotlin("jvm") version "1.9.25"
    kotlin("plugin.spring") version "1.9.25"
    id("org.springframework.boot") version "3.3.8"
    id("io.spring.dependency-management") version "1.1.7"
    kotlin("plugin.jpa") version "1.9.25"
}

group = "altea-si"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

extra["springCloudVersion"] = "2023.0.6"

// Surcharge la version log4j geree par Spring Boot 3.3.8 (2.23.1), vulnerable a
// l'encodage JSON des flottants non finis dans MapMessage.asJson()
// (suite incomplete de CVE-2026-34481). Non atteignable ici (pas de log4j-core
// ni de JsonTemplateLayout), corrige pour la conformite SCA.
extra["log4j2.version"] = "2.26.1"

// Monte Spring Framework 6.1.16 -> 6.1.21 (spring-web et spring-webflux inclus).
// Les modules spring-core / -web / -webflux / -context partagent la meme version :
// surcharger un seul de ces jars provoquerait des NoSuchMethodError au runtime.
// 6.1.21 est le correctif de la branche 6.1.x, celle contre laquelle Boot 3.3.8 est
// construit : on reste dans la matrice de compatibilite officielle (a la difference
// de 6.2.19, correctif de l'autre branche).
extra["spring-framework.version"] = "6.1.21"

dependencies {
    // Gateway
    implementation("org.springframework.cloud:spring-cloud-starter-gateway")
    
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")

    // Swagger / OpenAPI Aggregation
    implementation("org.springdoc:springdoc-openapi-starter-webflux-ui:2.3.0")
    
    // Kotlin support
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    
    // Observability & K8s Health Checks
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:${property("springCloudVersion")}")
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs += "-Xjsr305=strict"
        jvmTarget = "21"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}