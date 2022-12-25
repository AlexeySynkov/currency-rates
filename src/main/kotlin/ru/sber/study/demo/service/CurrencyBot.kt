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
const val RUB_TO_USD = "Перевод рубля в доллар"
const val USD_TO_RUB = "Перевод доллара в рубли"
const val RUB_TO_JPY = "Перевод рубля в юань"
const val JPY_TO_RUB = "Перевод юаня в рубли"
const val RUB_TO_EUR = "Перевод рубля в евро"
const val EUR_TO_RUB = "Перевод евро в рубли"


@Service
class CurrencyBot(
    private val currencyService: CurrencyRequestService
) : TelegramLongPollingBot() {

    private val convertList = listOf(RUB_TO_USD,USD_TO_RUB, RUB_TO_JPY ,JPY_TO_RUB, RUB_TO_EUR, EUR_TO_RUB)

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
                                    listOf(KeyboardButton(RUB_TO_USD), KeyboardButton(USD_TO_RUB)))))
                        }

                    CNY.currencyName -> "Выберите операцию"
                        .also {
                            keyBoard = ReplyKeyboardMarkup(
                                listOf(KeyboardRow(
                                    listOf(KeyboardButton(RUB_TO_JPY), KeyboardButton(JPY_TO_RUB)))))
                        }

                    EUR.currencyName -> "Выберите операцию"
                        .also {
                            keyBoard = ReplyKeyboardMarkup(
                                listOf(KeyboardRow(
                                    listOf(KeyboardButton(RUB_TO_EUR), KeyboardButton(EUR_TO_RUB)))))
                        }

                    else ->  {
                        if (convertList.contains(messageText)) SUM
                            .also { userRepository.setUserState(chatId, UserState.getByValue(messageText)) }
                        else
                            if (checkSum(messageText)) currencyService.convertSum(messageText, userRepository.getUserState(chatId))
                            else "Вы написали: *$messageText*"
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
