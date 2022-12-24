package ru.sber.study.demo.enum

enum class Currency(val currencyName: String, val code: String) {
    RUB("Рубль", "USDRUB,EURRUB,CNYRUB"),
    USD("Доллар", "RUBUSD,EURUSD,CNYUSD"),
    EUR("Евро", "RUBEUR,USDEUR,CNYEUR"),
    CNY("Юань", "RUBCNY,EURCNY,USDCNY")
}