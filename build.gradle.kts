plugins {
    application
    kotlin("jvm") version "1.7.20"
}

group = "ru.sber.study.demo"
version = "0.0.1"
application {
    mainClass.set("ru.sber.study.demo.CurrencyRatesApplicationKt")
}

repositories {
    mavenCentral()
}

val restAssuredVersion: String by extra { "4.4.0" }
dependencies {
    implementation("eu.vendeli:telegram-bot:2.4.2")
    implementation("io.rest-assured:kotlin-extensions:${restAssuredVersion}")
    implementation("io.rest-assured:rest-assured-all:${restAssuredVersion}")
    implementation("io.rest-assured:rest-assured:${restAssuredVersion}")
    implementation("io.rest-assured:json-path:${restAssuredVersion}")
    implementation("io.rest-assured:json-schema-validator:${restAssuredVersion}")
}