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

    // Кэш сообщений по номеру телефона — каждый номер хранит свои SMS
    private val _messagesByPhone = mutableMapOf<String, List<SmsMessage>>()

    // Локально добавленные (ещё не в БД) — тоже по номеру
    private val _localByPhone = mutableMapOf<String, MutableList<SmsMessage>>()

    private val _messages = MutableStateFlow<List<SmsMessage>>(emptyList())
    val messages: StateFlow<List<SmsMessage>> = _messages.asStateFlow()

    private val _hasPermission = MutableStateFlow(false)
    val hasPermission: StateFlow<Boolean> = _hasPermission.asStateFlow()

    private val _selectedAi = MutableStateFlow(AiAssistant.CHATGPT)
    val selectedAi: StateFlow<AiAssistant> = _selectedAi.asStateFlow()

    // Текущий активный номер
    private var currentPhone: String = ""

    fun setPermissionGranted(granted: Boolean) {
        _hasPermission.value = granted
    }

    fun selectAi(ai: AiAssistant) {
        _selectedAi.value = ai
        applyFilter()
    }

    fun loadMessages(context: Context, smsPhone: String, clearFirst: Boolean = false) {
        currentPhone = smsPhone
        if (clearFirst) {
            _messagesByPhone.remove(smsPhone)
            _localByPhone.remove(smsPhone)
        }
        viewModelScope.launch(Dispatchers.IO) {
            val result = mutableListOf<SmsMessage>()
            val phoneVariants = normalizePhoneVariants(smsPhone)
            val selection = phoneVariants.joinToString(" OR ") { "${Telephony.Sms.ADDRESS} = ?" }
            val selectionArgs = phoneVariants.toTypedArray()

            // Входящие
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

            // Исходящие
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

            val local = _localByPhone[smsPhone] ?: emptyList()
            val fromDb = result.sortedBy { it.date }
            // Мёрджим локальные с БД, убираем дубли по тексту+направлению
            val merged = (local + fromDb)
                .distinctBy { "${it.isIncoming}_${it.body.take(50)}" }
                .sortedBy { it.date }

            kotlinx.coroutines.withContext(Dispatchers.Main) {
                _messagesByPhone[smsPhone] = merged
                // Показываем только если этот номер всё ещё активен
                if (currentPhone == smsPhone) applyFilter()
            }
        }
    }

    // Добавляет исходящее локально — привязано к конкретному номеру
    fun addOutgoingMessage(body: String, phone: String = currentPhone) {
        val msg = SmsMessage(
            id = -System.currentTimeMillis(),
            body = body,
            date = System.currentTimeMillis(),
            isIncoming = false
        )
        _localByPhone.getOrPut(phone) { mutableListOf() }.add(msg)
        val current = (_messagesByPhone[phone] ?: emptyList()) + msg
        _messagesByPhone[phone] = current.sortedBy { it.date }
        if (currentPhone == phone) applyFilter()
    }

    // Добавляет входящее — привязано к текущему номеру
    fun addIncomingMessage(body: String, date: Long = System.currentTimeMillis()) {
        viewModelScope.launch(Dispatchers.Main) {
            val phone = currentPhone
            val msg = SmsMessage(id = date, body = body, date = date, isIncoming = true)
            val current = (_messagesByPhone[phone] ?: emptyList()) + msg
            _messagesByPhone[phone] = current.sortedBy { it.date }
            if (currentPhone == phone) applyFilter()
        }
    }

    private fun applyFilter() {
        val ai = _selectedAi.value
        val prefix = "[ ${ai.displayName} ]"
        val all = _messagesByPhone[currentPhone] ?: emptyList()
        _messages.value = all.filter { it.body.contains(prefix) }.sortedBy { it.date }
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
