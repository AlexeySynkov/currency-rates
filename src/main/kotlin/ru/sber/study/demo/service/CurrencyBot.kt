package ru.sber.study.demo.service

import groovy.xml.MarkupBuilder
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow
import ru.sber.study.demo.api.CurrencyRequestService
import ru.sber.study.demo.enum.Currency
import ru.sber.study.demo.enum.Currency.*
import ru.sber.study.demo.enum.UserState
import ru.sber.study.demo.enum.UserState.CONVERTING
import ru.sber.study.demo.enum.UserState.STARTED
import ru.sber.study.demo.repository.UserRepository

const val SHOW_COURSE = "Показать курс валют"
const val START_CONVERTER = "Конвертер валют"
const val SUM = "Введите сумму"

@Service
class CurrencyBot(
    private val currencyService: CurrencyRequestService
) : TelegramLongPollingBot() {

    private val convertList = listOf("Перевод рубля в доллар", "Перевод доллара в рубли","Перевод рубля в юань", "Перевод юаня в рубли", "Перевод рубля в евро", "Перевод евро в рубли")

    @Autowired
    private lateinit var userRepository: UserRepository

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
            var keyBoard: ReplyKeyboardMarkup? = null

            val responseText = if (message.hasText()) {
                when (val messageText = message.text) {
                    "/start" -> "Добро пожаловать!"
                        .also {
                            userRepository.setUserState(chatId, STARTED)
                            keyBoard = ReplyKeyboardMarkup(
                                listOf(KeyboardRow(
                                    listOf(KeyboardButton(SHOW_COURSE), KeyboardButton(START_CONVERTER)))))
                        }

                    "/get" -> "Получили набор валют"
                        .also {
                            val currency = currencyService.getCurrencyExchangeRate(RUB)
                            logger.info(currency.toString())
                        }

                    SHOW_COURSE -> "Должны получить набор валют"

                    START_CONVERTER -> "Выберите валюту для конвертации"
                        .also {
                            userRepository.setUserState(chatId, CONVERTING)
                            keyBoard = ReplyKeyboardMarkup(
                                values()
                                    .filter { it != RUB }
                                    .map {
                                    KeyboardRow(listOf(KeyboardButton(it.currencyName)).toList())
                                })
                        }

                    USD.currencyName -> "Выберите операцию"
                        .also {
                            keyBoard = ReplyKeyboardMarkup(
                                listOf(KeyboardRow(
                                    listOf(KeyboardButton("Перевод рубля в доллар", ), KeyboardButton("Перевод доллара в рубли")))))
                        }

                    CNY.currencyName -> "Выберите операцию"
                        .also {
                            keyBoard = ReplyKeyboardMarkup(
                                listOf(KeyboardRow(
                                    listOf(KeyboardButton("Перевод рубля в юань "), KeyboardButton("Перевод юаня в рубли")))))
                        }

                    EUR.currencyName -> "Выберите операцию"
                        .also {
                            keyBoard = ReplyKeyboardMarkup(
                                listOf(KeyboardRow(
                                    listOf(KeyboardButton("Перевод рубля в евро"), KeyboardButton("Перевод евро в рубли")))))
                        }

                    else ->  {
                        if (convertList.contains(messageText)) SUM
                        else
                            if (checkSum(messageText)) "ТУТ ВЫЗЫВАЕМ СЕРВИС КОНВЕРТАЦИИ" else "Вы написали: *$messageText*"
                    }
                }
            } else {
                "Я понимаю только текст"
            }

            sendNotification(chatId, responseText, keyBoard)
        }
    }

    private fun sendNotification(chatId: Long, responseText: String, keyboard: ReplyKeyboardMarkup?) {
        val responseMessage = SendMessage(chatId.toString(), responseText)
        keyboard?.resizeKeyboard = true
        responseMessage.replyMarkup = keyboard
        responseMessage.enableMarkdown(true)
        try {
            execute(responseMessage)
        } catch (e: Exception) {
            logger.error("Ошибка при отправке сообщения", e)
        }
    }

    private fun checkSum(string: String): Boolean {
        return try {
            string.toDouble()
            true
        } catch (e: Exception) {
            false
        }
    }
}
