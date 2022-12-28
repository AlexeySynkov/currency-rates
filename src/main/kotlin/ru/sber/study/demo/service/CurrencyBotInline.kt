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
import ru.sber.study.demo.enum.Currency
import ru.sber.study.demo.enum.UserState
import ru.sber.study.demo.repository.UserRepository

@Service
@Profile("default")
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
                    "/start" -> {
                        val userData = userRepository.getUserData(chatId)
                        if (userData.botMessageId != null) {
                            execute(DeleteMessage(chatId.toString(), userData.botMessageId!!))
                            userData.botMessageId = null
                        }
                        showCurrencyButtons(chatId, "Выберите валюту")
                        userRepository.setUserState(chatId, UserState.STARTED)
                        execute(DeleteMessage(chatId.toString(), message.messageId))
                    }

                    else -> {
                        execute(DeleteMessage(chatId.toString(), message.messageId))
                        if (userRepository.getUserState(chatId) == UserState.CONVERTING) {
                            checkSum(chatId, message.text)
                            showConverting(chatId)
                        }
                    }
                }
            }
        } else if (update.hasCallbackQuery()) {
            val callback = update.callbackQuery
            val chatId = callback.from.id
            if (callback.data != null) {
                val userData = userRepository.getUserData(chatId)
                if (userData.state == UserState.STARTED) {
                    userRepository.setUserState(chatId, UserState.GETTING_COURSES)
                    val currency = Currency.valueOf(callback.data)
                    val currencyInfo = currencyService.getCurrencyExchangeRate(currency)
                    userData.apply {
                        this.currency = currency
                        this.currencyInfo = currencyInfo
                    }
                    showCurrencyInfo(chatId)
                } else if (userData.state == UserState.GETTING_COURSES) {
                    if (callback.data == "BACK_TO_CURRENCIES") {
                        userRepository.setUserState(chatId, UserState.STARTED)
                        userData.apply {
                            this.currency = null
                            this.currencyInfo = null
                        }
                        showCurrencyButtons(chatId, "Выберите валюту")
                    } else if (callback.data == "CONVERTING") {
                        userRepository.setUserState(chatId, UserState.CONVERTING)
                        showConverting(chatId)
                    }
                } else if (userData.state == UserState.CONVERTING) {
                    if (callback.data == "BACK_TO_GETTING_COURSES") {
                        userRepository.setUserState(chatId, UserState.GETTING_COURSES)
                        userData.apply {
                            amount = null
                            currencyToConvert = null
                        }
                        showCurrencyInfo(chatId)
                    } else {
                        val currencyToConvert = Currency.valueOf(callback.data)
                        userData.currencyToConvert = currencyToConvert
                        showConverting(chatId)
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

    private fun showCurrencyButtons(chatId: Long, text: String) {
        val userData = userRepository.getUserData(chatId)
        if (userData.botMessageId == null) {
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
                this.messageId = userData.botMessageId
                this.text = text
                enableMarkdown(true)
                replyMarkup = createCurrencyButtons()
            }
            execute(message)
            logger.info("Изменено сообщение: {}", message)
        }
    }

    private fun showCurrencyInfo(chatId: Long) {
        val userData = userRepository.getUserData(chatId)

        var text = ""
        userData.currencyInfo!!.map {
            text = buildString {
                append(text)
                append(Currency.valueOf(it.key.substring(0, 3)).emojiCode)
                append(" ")
                append(Currency.valueOf(it.key.substring(0, 3)).name)
                append("/")
                append(userData.currency!!.name)
                append(" = ")
                append(format(it.value.toDouble(), 4))
                append("\n")
            }
        }

        if (userData.botMessageId == null) {
            val message = SendMessage().apply {
                this.chatId = chatId.toString()
                this.text = text
                enableMarkdown(true)
                replyMarkup = createCoursesButtons(userData.currency!!)
            }
            val result = execute(message)
            logger.info("Отправлено сообщение: {}", result)
            userRepository.getUserData(chatId).botMessageId = result.messageId
        } else {
            val message = EditMessageText().apply {
                this.chatId = chatId.toString()
                this.messageId = userData.botMessageId
                this.text = text
                enableMarkdown(true)
                replyMarkup = createCoursesButtons(userData.currency!!)
            }
            execute(message)
            logger.info("Изменено сообщение: {}", message)
        }
    }

    private fun showConverting(chatId: Long) {
        val userData = userRepository.getUserData(chatId)
        if (userData.currency == null || userData.currencyInfo == null) {
            sendNotification(chatId, "/start")
        }

        val text = if (userData.amount == null) {
            "Введите сумму валюты \"${userData.currency!!.currencyName}\" ${userData.currency!!.emojiCode}"
        } else {
            if (userData.currencyToConvert == null) {
                "${userData.currency!!.emojiCode} ${format(userData.amount!!, 4)}\n" +
                        "Выберите валюту, в которую нужно конвертировать"
            } else {
                val course =
                    userData.currencyInfo!!["${userData.currencyToConvert}${userData.currency}"]!!.toDouble()
                "${userData.currency!!.emojiCode} ${format(userData.amount!!, 4)} = " +
                        "${userData.currencyToConvert!!.emojiCode} ${format(userData.amount!! / course, 4)}"
            }
        }

        if (userData.botMessageId == null) {
            val message = SendMessage().apply {
                this.chatId = chatId.toString()
                this.text = text
                enableMarkdown(true)
                replyMarkup = createConvertButtons(chatId, userData.amount)
            }
            val result = execute(message)
            logger.info("Отправлено сообщение: {}", result)
            userRepository.getUserData(chatId).botMessageId = result.messageId
        } else {
            val message = EditMessageText().apply {
                this.chatId = chatId.toString()
                this.messageId = userData.botMessageId
                this.text = text
                enableMarkdown(true)
                replyMarkup = createConvertButtons(chatId, userData.amount)
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

    private fun createCoursesButtons(currency: Currency): InlineKeyboardMarkup {
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

    private fun createConvertButtons(chatId: Long, amount: Double?): InlineKeyboardMarkup {
        val userData = userRepository.getUserData(chatId)

        val replyKeyboardMarkup = InlineKeyboardMarkup()

        var currencyButtonList: List<InlineKeyboardButton>? = null
        if (amount != null) {
            currencyButtonList = Currency.values()
                .filter {
                    it != userData.currency && it != userData.currencyToConvert
                }
                .map { currency ->
                    InlineKeyboardButton().apply {
                        text = "${currency.emojiCode} ${currency.currencyName}"
                        callbackData = currency.name
                    }
                }
        }

        val buttonBackToCurrencies = InlineKeyboardButton().apply {
            text = "Вернуться к курсам ${userData.currency!!.emojiCode}"
            callbackData = "BACK_TO_GETTING_COURSES"
        }

        if (currencyButtonList != null) {
            replyKeyboardMarkup.keyboard = listOf(currencyButtonList, listOf(buttonBackToCurrencies))
        } else {
            replyKeyboardMarkup.keyboard = listOf(listOf(buttonBackToCurrencies))
        }

        return replyKeyboardMarkup
    }

    private fun checkSum(chatId: Long, text: String): Boolean {
        val userData = userRepository.getUserData(chatId)
        return try {
            userData.amount = text.replace(",", ".").toDouble()
            true
        } catch (e: Exception) {
            logger.error("Пользователь неправильно указал число", e)
            false
        }
    }

    fun format(input: Double, scale: Int) = "%.${scale}f".format(input)
}
