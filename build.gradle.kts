import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "2.7.7"
    id("io.spring.dependency-management") version "1.0.15.RELEASE"
    kotlin("jvm") version "1.6.21"
    kotlin("plugin.spring") version "1.6.21"
}

group = "ru.sber.study"
version = "1.0.0-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_11

repositories {
    mavenCentral()
}

val restAssuredVersion: String by extra { "4.4.0" }

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    implementation("org.telegram:telegrambots-spring-boot-starter:6.1.0")

    implementation("io.rest-assured:kotlin-extensions:${restAssuredVersion}")
    implementation("io.rest-assured:rest-assured-all:${restAssuredVersion}")
    implementation("io.rest-assured:rest-assured:${restAssuredVersion}")
    implementation("io.rest-assured:json-path:${restAssuredVersion}")
    implementation("io.rest-assured:json-schema-validator:${restAssuredVersion}")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "11"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}