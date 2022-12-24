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

dependencies {
    implementation("eu.vendeli:telegram-bot:2.4.2")
}