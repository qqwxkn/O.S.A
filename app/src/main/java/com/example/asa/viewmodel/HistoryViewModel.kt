package com.example.asa.viewmodel

import android.content.Context
import android.provider.Telephony
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.asa.model.AiAssistant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SmsMessage(
    val id: Long,
    val body: String,
    val date: Long,
    val isIncoming: Boolean
)

class HistoryViewModel : ViewModel() {

    private val _allMessages = MutableStateFlow<List<SmsMessage>>(emptyList())

    private val _messages = MutableStateFlow<List<SmsMessage>>(emptyList())
    val messages: StateFlow<List<SmsMessage>> = _messages.asStateFlow()

    private val _hasPermission = MutableStateFlow(false)
    val hasPermission: StateFlow<Boolean> = _hasPermission.asStateFlow()

    private val _selectedAi = MutableStateFlow(AiAssistant.CHATGPT)
    val selectedAi: StateFlow<AiAssistant> = _selectedAi.asStateFlow()

    fun setPermissionGranted(granted: Boolean) {
        _hasPermission.value = granted
    }

    fun selectAi(ai: AiAssistant) {
        _selectedAi.value = ai
        applyFilter()
    }

    fun loadMessages(context: Context, smsPhone: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = mutableListOf<SmsMessage>()
            val phoneVariants = normalizePhoneVariants(smsPhone)
            val selection = phoneVariants.joinToString(" OR ") { "${Telephony.Sms.ADDRESS} = ?" }
            val selectionArgs = phoneVariants.toTypedArray()

            // Входящие (ответы от AI)
            context.contentResolver.query(
                Telephony.Sms.Inbox.CONTENT_URI,
                arrayOf(Telephony.Sms._ID, Telephony.Sms.BODY, Telephony.Sms.DATE),
                selection, selectionArgs,
                "${Telephony.Sms.DATE} DESC"
            )?.use { cursor ->
                while (cursor.moveToNext()) {
                    result.add(SmsMessage(cursor.getLong(0), cursor.getString(1) ?: "", cursor.getLong(2), true))
                }
            }

            // Исходящие (запросы пользователя)
            context.contentResolver.query(
                Telephony.Sms.Sent.CONTENT_URI,
                arrayOf(Telephony.Sms._ID, Telephony.Sms.BODY, Telephony.Sms.DATE),
                selection, selectionArgs,
                "${Telephony.Sms.DATE} DESC"
            )?.use { cursor ->
                while (cursor.moveToNext()) {
                    result.add(SmsMessage(cursor.getLong(0), cursor.getString(1) ?: "", cursor.getLong(2), false))
                }
            }

            // Мёрджим с локально добавленными — убираем дубли по тексту+направлению
            val existing = _allMessages.value.filter { it.id < 0 } // локальные (id < 0)
            val fromDb = result.sortedBy { it.date }
            val merged = (existing + fromDb)
                .distinctBy { "${it.isIncoming}_${it.body.take(50)}" }
                .sortedBy { it.date }

            // Переключаемся на Main для обновления StateFlow
            kotlinx.coroutines.withContext(Dispatchers.Main) {
                _allMessages.value = merged
                applyFilter()
            }
        }
    }

    // Вызывается сразу после отправки SMS — добавляет исходящее локально без перечитывания БД
    fun addOutgoingMessage(body: String) {
        val msg = SmsMessage(
            id = -System.currentTimeMillis(), // отрицательный id = локальное, ещё не из БД
            body = body,
            date = System.currentTimeMillis(),
            isIncoming = false
        )
        _allMessages.value = (_allMessages.value + msg).sortedBy { it.date }
        applyFilter()
    }

    // Вызывается из BroadcastReceiver при получении входящего SMS
    fun addIncomingMessage(body: String, date: Long = System.currentTimeMillis()) {
        // Принудительно в Main потоке — BroadcastReceiver может вызвать из любого потока
        viewModelScope.launch(Dispatchers.Main) {
            val msg = SmsMessage(
                id = date,
                body = body,
                date = date,
                isIncoming = true
            )
            _allMessages.value = (_allMessages.value + msg).sortedBy { it.date }
            applyFilter()
        }
    }

    // Все SMS (и входящие и исходящие) содержат префикс "[ DisplayName ]"
    // Фильтруем по префиксу, показываем от старых к новым (запрос сверху, ответы снизу)
    private fun applyFilter() {
        val ai = _selectedAi.value
        val prefix = "[ ${ai.displayName} ]"
        val all = _allMessages.value

        _messages.value = all
            .filter { it.body.contains(prefix) }
            .sortedBy { it.date } // старые сверху — запрос выше ответа
    }

    private fun normalizePhoneVariants(phone: String): List<String> {
        val digits = phone.filter { it.isDigit() }
        return when {
            digits.startsWith("8") && digits.length == 11 -> listOf(phone, "+7" + digits.drop(1))
            digits.startsWith("7") && digits.length == 11 -> listOf(phone, "8" + digits.drop(1), "+$digits")
            else -> listOf(phone)
        }
    }
}
