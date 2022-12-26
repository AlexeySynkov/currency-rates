package ru.sber.study.demo.repository

import ru.sber.study.demo.enum.Currency
import ru.sber.study.demo.enum.UserState

data class UserData(
    var state: UserState,
    var userMessageId: Int? = null,
    var botMessageId: Int? = null,
    var currency: Currency? = null,
    var currencyInfo: Map<String, String>? = null,
    var amount: Double? = null,
    var currencyToConvert: Currency? = null
)
