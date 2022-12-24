package ru.sber.study.demo.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update
import ru.sber.study.demo.api.CurrencyRequestService
import ru.sber.study.demo.enum.Currency

@Service
class CurrencyBot(
    private val currencyService: CurrencyRequestService
) : TelegramLongPollingBot() {

    companion object {
        private val logger = LoggerFactory.getLogger(CurrencyBot::class.java)
    }

    @Value("\${telegram.botName}")
    private val botName: String = ""

    @Value("\${telegram.token}")
    private val token: String = ""

    override fun getBotToken() = token

    override fun getBotUsername() = botName

    override fun onUpdateReceived(update: Update) {
        logger.info("Получили сообщение: {}", update)
        if (update.hasMessage()) {
            val message = update.message
            val chatId = message.chatId
            val responseText = if (message.hasText()) {
                when (val messageText = message.text) {
                    "/start" -> "Добро пожаловать!"
                    "/get" -> "Получили набор валют"
                        .also {
                            val currency = currencyService.getCurrencyExchangeRate(Currency.RUB)
                            logger.info(currency.toString())
                        }

                    else -> "Вы написали: *$messageText*"
                }
            } else {
                "Я понимаю только текст"
            }

            sendNotification(chatId, responseText)
        }
    }

    private fun sendNotification(chatId: Long, responseText: String) {
        val responseMessage = SendMessage(chatId.toString(), responseText)
        responseMessage.enableMarkdown(true)
        try {
            execute(responseMessage)
        } catch (e: Exception) {
            logger.error("Ошибка при отправке сообщения", e)
        }
    }
}
