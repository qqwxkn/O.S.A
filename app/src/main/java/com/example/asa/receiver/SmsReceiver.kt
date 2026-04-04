package com.example.asa.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log

class SmsReceiver(
    private val smsPhone: String,
    private val onMessageReceived: (body: String, date: Long) -> Unit
) : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("SmsReceiver", "onReceive action=${intent.action}")
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        Log.d("SmsReceiver", "messages count=${messages?.size}, watching=$smsPhone")
        if (messages.isNullOrEmpty()) return

        // Группируем части по отправителю и склеиваем в одно сообщение
        val grouped = messages.groupBy { it.originatingAddress ?: "" }
        grouped.forEach { (from, parts) ->
            Log.d("SmsReceiver", "from=$from, isSame=${isSamePhone(from, smsPhone)}")
            if (isSamePhone(from, smsPhone)) {
                // Склеиваем все PDU-части в одно тело
                val body = parts.joinToString("") { it.messageBody ?: "" }
                val date = parts.first().timestampMillis
                Log.d("SmsReceiver", "MATCH! body=${body.take(30)}, parts=${parts.size}")
                onMessageReceived(body, date)
            }
        }
    }

    private fun isSamePhone(a: String, b: String): Boolean {
        val digitsA = a.filter { it.isDigit() }.takeLast(10)
        val digitsB = b.filter { it.isDigit() }.takeLast(10)
        return digitsA == digitsB
    }
}
