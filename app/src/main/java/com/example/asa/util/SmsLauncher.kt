package com.example.asa.util

import android.content.Context
import android.content.Intent
import android.net.Uri

object SmsLauncher {
    fun launch(context: Context, phone: String, text: String) {
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$phone")).apply {
            putExtra("sms_body", text)
        }
        context.startActivity(intent)
    }
}
