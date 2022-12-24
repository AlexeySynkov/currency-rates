package ru.sber.study.demo.api

import io.restassured.RestAssured
import io.restassured.builder.RequestSpecBuilder
import io.restassured.config.LogConfig
import io.restassured.config.RestAssuredConfig
import io.restassured.filter.log.LogDetail
import io.restassured.http.ContentType
import io.restassured.specification.RequestSpecification
import org.apache.http.HttpStatus
import ru.sber.study.demo.enum.Currency

class CurrencyRequestService {

    val rates = "rates"
    //ключ можно получить при регистрации на currate.ru
    val apiKey = "cd429f01dce65a113533c9ddc9d29086"

    private val restAssuredConfig: RestAssuredConfig = RestAssuredConfig.config()
        .logConfig(LogConfig.logConfig().enableLoggingOfRequestAndResponseIfValidationFails(LogDetail.ALL))

    private fun requestSpecification(): RequestSpecification? = RequestSpecBuilder()
        .setBaseUri("https://currate.ru/api/")
        .log(LogDetail.ALL)
        .build()

    fun startWithRestSpecification(): RequestSpecification = RestAssured.given()
        .spec(requestSpecification())
        .contentType(ContentType.JSON)
        .config(restAssuredConfig)

    //несколько валютных пар передаются через запятую, например: "USDRUB,EURRUB,USDJPY"
    fun getCurrencyExchangeRate(pairs: Currency): MutableMap<String,String> = startWithRestSpecification()
        .queryParams(mapOf("get" to rates, "pairs" to pairs.code, "key" to apiKey))
        .get()
        .then()
        .statusCode(HttpStatus.SC_OK)
        .extract().jsonPath().getMap("data")
}