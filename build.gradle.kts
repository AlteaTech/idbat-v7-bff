plugins {
    kotlin("jvm") version "1.9.25"
    kotlin("plugin.spring") version "1.9.25"
    id("org.springframework.boot") version "3.5.16"
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

// Boot 3.5.16 + Cloud 2025.0.3 fournissent nativement spring-security 6.5.11,
// spring-framework 6.2.19 et spring-cloud-gateway 4.3.5. Cette combinaison couvre
// l'exigence spring-security-web >= 6.5.9 et ferme CVE-2026-47825, qui n'avait
// aucun correctif OSS sur la branche gateway 4.1.x. Aucune surcharge de version
// n'est donc necessaire pour ces trois composants.
extra["springCloudVersion"] = "2025.0.3"

// Seule surcharge restante : Boot 3.5.16 gere log4j 2.24.3, encore vulnerable a
// l'encodage JSON des flottants non finis dans MapMessage.asJson() (suite
// incomplete de CVE-2026-34481). Non atteignable ici (pas de log4j-core ni de
// JsonTemplateLayout), monte pour la conformite SCA.
extra["log4j2.version"] = "2.26.1"

dependencies {
    // Gateway
    implementation("org.springframework.cloud:spring-cloud-starter-gateway")
    
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")

    // Swagger / OpenAPI Aggregation
    implementation("org.springdoc:springdoc-openapi-starter-webflux-ui:2.8.17")
    
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
