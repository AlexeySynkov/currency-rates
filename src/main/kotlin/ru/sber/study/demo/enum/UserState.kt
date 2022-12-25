package ru.sber.study.demo.enum

enum class UserState(private val value: String = "") {
    NOT_REGISTERED,
    STARTED,
    CONVERTING,
    GETTING_COURSES,
    RUB_TO_USD("Перевод рубля в доллар"),
    RUB_TO_EUR("Перевод рубля в евро"),
    USD_TO_RUB("Перевод доллара в рубли"),
    RUB_TO_JPY("Перевод рубля в юань"),
    JPY_TO_RUB("Перевод юаня в рубли"),
    EUR_TO_RUB("Перевод евро в рубли");

    companion object Util {
        fun getByValue(value: String): UserState {
            return values().findLast { it.value == value } ?: NOT_REGISTERED
        }
    }
}