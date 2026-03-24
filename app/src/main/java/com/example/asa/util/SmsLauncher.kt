package com.example.asa.util

import android.content.Context
import android.telephony.SmsManager

object SmsLauncher {

    // Отправляет SMS напрямую без открытия приложения сообщений
    fun sendDirect(context: Context, phone: String, text: String) {
        val smsManager = context.getSystemService(SmsManager::class.java)
        // Разбиваем на части если текст длинный
        val parts = smsManager.divideMessage(text)
        if (parts.size == 1) {
            smsManager.sendTextMessage(phone, null, text, null, null)
        } else {
            smsManager.sendMultipartTextMessage(phone, null, parts, null, null)
        }
    }

    // Оставляем старый метод на случай отката
    fun launch(context: Context, phone: String, text: String) {
        sendDirect(context, phone, text)
    }
}
