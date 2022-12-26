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
import ru.sber.study.demo.enum.UserState
import ru.sber.study.demo.enum.UserState.*

@Service
class CurrencyRequestService {

    companion object {
        fun getCurrencyExchangeRateMock(pairs: Currency): MutableMap<String, String> = mutableMapOf(
            "USDRUB" to "64.1824",
            "EURRUB" to "69.244",
            "CNYRUB" to "9.13811"
        )
    }

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

    // Тут пока просто так вывожу с числом, потому что не знаю, как выглядит мапа и не могу получить курс от getCurrencyExchangeRate
    fun convertSum(sum: String, state: UserState): String {
        return when (state) {
//            UserState.RUB_TO_EUR -> sum.toDouble() * getCurrencyExchangeRate(Currency.RUB)[Currency.EUR.currencyName]!!.toDouble()
            RUB_TO_EUR -> (sum.toDouble() * 70).toString()
            RUB_TO_USD -> (sum.toDouble() * 70).toString()
            USD_TO_RUB -> (sum.toDouble() * 70).toString()
            RUB_TO_JPY -> (sum.toDouble() * 70).toString()
            JPY_TO_RUB -> (sum.toDouble() * 70).toString()
            EUR_TO_RUB -> (sum.toDouble() * 70).toString()
            else -> "Не удалось получить курс валют"
        }
    }
}