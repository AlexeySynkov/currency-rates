package ru.sber.study.demo.enum

enum class Currency(val currencyName: String, val code: String, val emojiCode: String) {
    RUB("Рубль", "USDRUB,EURRUB,CNYRUB", "\uD83C\uDDF7\uD83C\uDDFA"),
    USD("Доллар", "RUBUSD,EURUSD,CNYUSD", "\uD83C\uDDFA\uD83C\uDDF8"),
    EUR("Евро", "RUBEUR,USDEUR,CNYEUR", "\uD83C\uDDEA\uD83C\uDDFA"),
    CNY("Юань", "RUBCNY,EURCNY,USDCNY", "\uD83C\uDDE8\uD83C\uDDF3")
}