package ru.sber.study.demo.repository

import ru.sber.study.demo.enum.UserState

data class UserData(
    var state: UserState,
    var userMessageId: Int? = null,
    var botMessageId: Int? = null
)
