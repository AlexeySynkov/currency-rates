package ru.sber.study.demo.api

import io.restassured.RestAssured
import io.restassured.builder.RequestSpecBuilder
import io.restassured.config.LogConfig
import io.restassured.config.RestAssuredConfig
import io.restassured.filter.log.LogDetail
import io.restassured.http.ContentType
import io.restassured.specification.RequestSpecification
import org.apache.http.HttpStatus
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import ru.sber.study.demo.enum.Currency

@Service
class CurrencyRequestService {

    @Value("\${curRate.get}")
    private val get: String = ""

    @Value("\${curRate.apiKey}")
    private val apiKey: String = ""

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
    fun getCurrencyExchangeRate(pairs: Currency): MutableMap<String, String> = startWithRestSpecification()
        .queryParams(mapOf("get" to get, "pairs" to pairs.code, "key" to apiKey))
        .get()
        .then()
        .statusCode(HttpStatus.SC_OK)
        .extract().jsonPath().getMap("data")
}