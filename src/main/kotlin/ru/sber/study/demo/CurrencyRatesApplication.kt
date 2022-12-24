package ru.sber.study.demo

import eu.vendeli.tgbot.TelegramBot

suspend fun main() {
    val bot = TelegramBot("BOT_TOKEN", "ru.sber.study.demo.controller")
    // And that's all write your own bot from blank.
    // Create new controllers in controller folder or expand old one

    bot.handleUpdates()
}
