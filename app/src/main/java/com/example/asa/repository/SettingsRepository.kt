package com.example.asa.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.asa.model.AiAssistant
import com.example.asa.model.AppTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface SettingsRepository {
    val themeFlow: Flow<AppTheme>
    val defaultAiFlow: Flow<AiAssistant>
    val smsPhoneFlow: Flow<String>
    val avatarUriFlow: Flow<String?>
    val availablePhonesFlow: Flow<List<Pair<String, String>>> // phone to label
    suspend fun setTheme(theme: AppTheme)
    suspend fun setDefaultAi(ai: AiAssistant)
    suspend fun setSmsPhone(phone: String)
    suspend fun setAvatarUri(uri: String)
    suspend fun setAvailablePhones(phones: List<Pair<String, String>>)
}

class DataStoreSettingsRepository(private val context: Context) : SettingsRepository {

    companion object {
        private val KEY_THEME = stringPreferencesKey("theme")
        private val KEY_DEFAULT_AI = stringPreferencesKey("default_ai")
        private val KEY_SMS_PHONE = stringPreferencesKey("sms_phone")
        private val KEY_AVATAR_URI = stringPreferencesKey("avatar_uri")
        private val KEY_PHONES_CACHE = stringPreferencesKey("phones_cache")

        private const val DEFAULT_PHONE = ""
        private const val DEFAULT_PHONES_CACHE = ""
    }

    override val themeFlow: Flow<AppTheme> = context.dataStore.data.map { prefs ->
        val value = prefs[KEY_THEME]
        if (value != null) {
            try { AppTheme.valueOf(value) } catch (_: IllegalArgumentException) { AppTheme.YELLOW }
        } else {
            AppTheme.YELLOW
        }
    }

    override val defaultAiFlow: Flow<AiAssistant> = context.dataStore.data.map { prefs ->
        val value = prefs[KEY_DEFAULT_AI]
        if (value != null) {
            try { AiAssistant.valueOf(value) } catch (_: IllegalArgumentException) { AiAssistant.CHATGPT }
        } else {
            AiAssistant.CHATGPT
        }
    }

    override val smsPhoneFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_SMS_PHONE] ?: DEFAULT_PHONE
    }

    override val avatarUriFlow: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[KEY_AVATAR_URI]?.takeIf { it.isNotEmpty() }
    }

    // Список номеров из кэша — формат "phone|label;phone|label"
    override val availablePhonesFlow: Flow<List<Pair<String, String>>> = context.dataStore.data.map { prefs ->
        val raw = prefs[KEY_PHONES_CACHE] ?: DEFAULT_PHONES_CACHE
        raw.split(";").mapNotNull { entry ->
            val parts = entry.split("|")
            if (parts.size == 2) Pair(parts[0], parts[1]) else null
        }
    }

    override suspend fun setTheme(theme: AppTheme) {
        context.dataStore.edit { it[KEY_THEME] = theme.name }
    }

    override suspend fun setDefaultAi(ai: AiAssistant) {
        context.dataStore.edit { it[KEY_DEFAULT_AI] = ai.name }
    }

    override suspend fun setSmsPhone(phone: String) {
        context.dataStore.edit { it[KEY_SMS_PHONE] = phone }
    }

    override suspend fun setAvatarUri(uri: String) {
        context.dataStore.edit { it[KEY_AVATAR_URI] = uri }
    }

    override suspend fun setAvailablePhones(phones: List<Pair<String, String>>) {
        val encoded = phones.joinToString(";") { "${it.first}|${it.second}" }
        context.dataStore.edit { it[KEY_PHONES_CACHE] = encoded }
    }
}
