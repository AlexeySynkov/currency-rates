package ru.sber.study.demo.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import ru.sber.study.demo.api.CurrencyRequestService
import ru.sber.study.demo.api.CurrencyRequestService.Companion.getCurrencyExchangeRateMock
import ru.sber.study.demo.enum.Currency
import ru.sber.study.demo.enum.UserState
import ru.sber.study.demo.repository.UserRepository

@Service
@Profile("michael")
class CurrencyBotInline(
    private val currencyService: CurrencyRequestService
) : TelegramLongPollingBot() {

    @Autowired
    private lateinit var userRepository: UserRepository

    companion object {
        private val logger = LoggerFactory.getLogger(CurrencyBot::class.java)
        private var userMessageId: Int? = null
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

            if (message.hasText()) {
                when (message.text) {
                    "/start" -> showCurrencyButtons(chatId, "Выберите валюту").also {
                        userRepository.setUserState(chatId, UserState.STARTED)
                        execute(DeleteMessage(chatId.toString(), message.messageId))
                    }

                    else -> execute(DeleteMessage(chatId.toString(), message.messageId))
                }
            }
        } else if (update.hasCallbackQuery()) {
            val callback = update.callbackQuery
            val chatId = callback.from.id
            if (callback.data != null) {
                val userData = userRepository.getUserData(chatId)
                if (userData.state == UserState.STARTED) {
                    val currency = Currency.valueOf(callback.data)
                    val currencyInfo = getCurrencyExchangeRateMock(currency)
                    userRepository.setUserState(chatId, UserState.GETTING_COURSES)
                    showCurrencyInfo(chatId, currency, currencyInfo, userData.botMessageId)
                } else if (userData.state == UserState.GETTING_COURSES) {
                    if (callback.data == "BACK_TO_CURRENCIES") {
                        userRepository.setUserState(chatId, UserState.STARTED)
                        showCurrencyButtons(chatId, "Выберите валюту", userData.botMessageId)
                    }
                }
            }
        }
    }

    private fun sendNotification(chatId: Long, responseText: String) {
        val responseMessage = SendMessage(chatId.toString(), responseText)
        try {
            val result = execute(responseMessage)
            userMessageId = result.messageId
            logger.info("Отправлено сообщение: {}", result)
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

    private fun showCurrencyButtons(chatId: Long, text: String, messageId: Int? = null) {
        if (messageId == null) {
            val message = SendMessage().apply {
                this.chatId = chatId.toString()
                this.text = text
                enableMarkdown(true)
                replyMarkup = createCurrencyButtons()
            }
            val result = execute(message)
            logger.info("Отправлено сообщение: {}", result)
            userRepository.getUserData(chatId).botMessageId = result.messageId
        } else {
            val message = EditMessageText().apply {
                this.chatId = chatId.toString()
                this.messageId = messageId
                this.text = text
                enableMarkdown(true)
                replyMarkup = createCurrencyButtons()
            }
            execute(message)
            logger.info("Изменено сообщение: {}", message)
        }
    }

    private fun showCurrencyInfo(
        chatId: Long,
        currency: Currency,
        currencyInfo: Map<String, String>,
        messageId: Int? = null
    ) {
        //TODO здесь красиво выводим курсы валют, полученные в переменной currencyInfo
        var text = ""
        currencyInfo.map {
            text = buildString {
                append(text)
                append(Currency.valueOf(it.key.toString().substring(0, 3)).emojiCode)
                append(" ")
                append(Currency.valueOf(it.key.toString().substring(0, 3)).currencyName)
                append(" ")
                append(it.value)
                append("\n")
            }
        }

        if (messageId == null) {
            val message = SendMessage().apply {
                this.chatId = chatId.toString()
                this.text = text
                enableMarkdown(true)
                replyMarkup = createConvertButtons(currency)
            }
            val result = execute(message)
            logger.info("Отправлено сообщение: {}", result)
            userRepository.getUserData(chatId).botMessageId = result.messageId
        } else {
            val message = EditMessageText().apply {
                this.chatId = chatId.toString()
                this.messageId = messageId
                this.text = text
                enableMarkdown(true)
                replyMarkup = createConvertButtons(currency)
            }
            execute(message)
            logger.info("Изменено сообщение: {}", message)
        }
    }

    private fun createCurrencyButtons(): InlineKeyboardMarkup {
        val replyKeyboardMarkup = InlineKeyboardMarkup()

        val buttonList = Currency.values()
            .map { currency ->
                InlineKeyboardButton().apply {
                    text = "${currency.emojiCode} ${currency.currencyName}"
                    callbackData = currency.name
                }
            }
            .chunked(2)


        replyKeyboardMarkup.keyboard = buttonList
        return replyKeyboardMarkup
    }

    private fun createConvertButtons(currency: Currency): InlineKeyboardMarkup {
        val replyKeyboardMarkup = InlineKeyboardMarkup()

        val buttonConvertCurrency = InlineKeyboardButton().apply {
            text = "Конвертация ${currency.emojiCode}"
            callbackData = "CONVERTING"
        }

        val buttonBackToCurrencies = InlineKeyboardButton().apply {
            text = "Вернуться к списку валют"
            callbackData = "BACK_TO_CURRENCIES"
        }

        replyKeyboardMarkup.keyboard = listOf(listOf(buttonConvertCurrency), listOf(buttonBackToCurrencies))
        return replyKeyboardMarkup
    }
}
