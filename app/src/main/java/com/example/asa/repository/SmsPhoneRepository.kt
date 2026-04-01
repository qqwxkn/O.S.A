package com.example.asa.repository

import com.example.asa.App
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SmsPhoneDto(
    @SerialName("phone") val phone: String,
    @SerialName("label") val label: String,
    @SerialName("is_active") val isActive: Boolean = true
)

class SmsPhoneRepository {
    private val table get() = App.supabase.postgrest.from("sms_phones")

    suspend fun fetchPhones(): Result<List<Pair<String, String>>> = try {
        val rows = table.select {
            filter { eq("is_active", true) }
        }.decodeList<SmsPhoneDto>()
        Result.success(rows.map { Pair(it.phone, it.label) })
    } catch (e: Exception) {
        Result.failure(e)
    }
}
