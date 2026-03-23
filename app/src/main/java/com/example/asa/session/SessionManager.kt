package com.example.asa.session

import android.content.Context
import com.example.asa.model.User

interface SessionManager {
    fun saveUserId(userId: String)
    fun getUserId(): String?
    fun clearSession()
    fun isLoggedIn(): Boolean
    fun saveUserCache(user: User)
    fun getUserCache(): User?
}

class SharedPreferencesSessionManager(context: Context) : SessionManager {

    private val prefs = context.getSharedPreferences("session_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_USER_ID = "user_id"
        private const val KEY_NICKNAME = "cache_nickname"
        private const val KEY_CREATED_AT = "cache_created_at"
        private const val KEY_REQUESTS_COUNT = "cache_requests_count"
        private const val KEY_LOGIN = "cache_login"
        private const val KEY_PHONE = "cache_phone"
        private const val KEY_VK_ID = "cache_vk_id"
        private const val KEY_AVATAR_URL = "cache_avatar_url"
        private const val KEY_HAS_PASSWORD = "cache_has_password"
    }

    override fun saveUserId(userId: String) {
        prefs.edit().putString(KEY_USER_ID, userId).apply()
    }

    override fun getUserId(): String? = prefs.getString(KEY_USER_ID, null)

    override fun clearSession() {
        prefs.edit().clear().apply()
    }

    override fun isLoggedIn(): Boolean = getUserId() != null

    override fun saveUserCache(user: User) {
        prefs.edit()
            .putString(KEY_NICKNAME, user.nickname)
            .putString(KEY_CREATED_AT, user.createdAt)
            .putInt(KEY_REQUESTS_COUNT, user.requestsCount)
            .putString(KEY_LOGIN, user.login)
            .putString(KEY_PHONE, user.phone)
            .putString(KEY_VK_ID, user.vkId)
            .putString(KEY_AVATAR_URL, user.avatarUrl)
            .putBoolean(KEY_HAS_PASSWORD, user.hasPassword)
            .apply()
    }

    override fun getUserCache(): User? {
        val id = getUserId() ?: return null
        val nickname = prefs.getString(KEY_NICKNAME, null) ?: return null
        return User(
            id = id,
            login = prefs.getString(KEY_LOGIN, "") ?: "",
            nickname = nickname,
            phone = prefs.getString(KEY_PHONE, "") ?: "",
            vkId = prefs.getString(KEY_VK_ID, null),
            avatarUrl = prefs.getString(KEY_AVATAR_URL, null),
            requestsCount = prefs.getInt(KEY_REQUESTS_COUNT, 0),
            createdAt = prefs.getString(KEY_CREATED_AT, "") ?: "",
            hasPassword = prefs.getBoolean(KEY_HAS_PASSWORD, false)
        )
    }
}
