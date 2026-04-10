package com.example.asa.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.asa.model.User
import com.example.asa.repository.SettingsRepository
import com.example.asa.repository.SupabaseUserRepository
import com.example.asa.repository.UserRepository
import com.example.asa.session.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val userRepository: UserRepository,
    private val supabaseRepo: SupabaseUserRepository? = null,
    private val sessionManager: SessionManager,
    private val settingsRepository: SettingsRepository,
    private val context: Context? = null
) : ViewModel() {

    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user.asStateFlow()

    private val _avatarUri = MutableStateFlow<Uri?>(null)
    val avatarUri: StateFlow<Uri?> = _avatarUri.asStateFlow()

    private val _avatarBitmap = MutableStateFlow<Bitmap?>(null)
    val avatarBitmap: StateFlow<Bitmap?> = _avatarBitmap.asStateFlow()

    init {
        _user.value = sessionManager.getUserCache()
        // Восстанавливаем аватарку из кэша сессии
        sessionManager.getUserCache()?.avatarUrl?.takeIf { it.isNotBlank() }?.let { url ->
            if (url.startsWith("data:image")) {
                _avatarBitmap.value = base64ToBitmap(url)
            } else {
                _avatarUri.value = Uri.parse(url)
            }
        }
        loadUser()
        viewModelScope.launch {
            settingsRepository.avatarUriFlow.collect { uriString ->
                if (!uriString.isNullOrBlank()) {
                    if (uriString.startsWith("data:image")) {
                        _avatarBitmap.value = base64ToBitmap(uriString)
                    } else {
                        _avatarUri.value = Uri.parse(uriString)
                    }
                }
            }
        }
    }

    fun loadUser() {
        viewModelScope.launch {
            val userId = sessionManager.getUserId() ?: return@launch
            userRepository.getUserById(userId).onSuccess { user ->
                _user.value = user
                sessionManager.saveUserCache(user)
                // Если в БД есть аватарка — берём из БД (это публичный URL или base64)
                if (!user.avatarUrl.isNullOrBlank()) {
                    if (user.avatarUrl.startsWith("data:image")) {
                        _avatarBitmap.value = base64ToBitmap(user.avatarUrl)
                    } else {
                        _avatarUri.value = Uri.parse(user.avatarUrl)
                    }
                    settingsRepository.setAvatarUri(user.avatarUrl)
                }
            }
        }
    }

    fun updateAvatarBitmap(bitmap: Bitmap) {
        viewModelScope.launch {
            val userId = sessionManager.getUserId() ?: return@launch
            val stream = java.io.ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
            val bytes = stream.toByteArray()
            val base64 = "data:image/jpeg;base64," + android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
            supabaseRepo?.updateAvatarUrl(userId, base64)
            settingsRepository.setAvatarUri(base64)
            _avatarBitmap.value = bitmap
            _avatarUri.value = null
        }
    }

    fun updateAvatar(uri: Uri) {
        _avatarUri.value = uri
        viewModelScope.launch {
            val userId = sessionManager.getUserId() ?: run {
                android.util.Log.e("OSA_AVATAR", "userId is null")
                return@launch
            }
            val ctx = context ?: run {
                android.util.Log.e("OSA_AVATAR", "context is null")
                settingsRepository.setAvatarUri(uri.toString())
                return@launch
            }
            try {
                val bytes = ctx.contentResolver.openInputStream(uri)?.readBytes() ?: run {
                    android.util.Log.e("OSA_AVATAR", "bytes is null")
                    return@launch
                }
                android.util.Log.d("OSA_AVATAR", "bytes size: ${bytes.size}")
                val base64 = "data:image/jpeg;base64," + android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                val result = supabaseRepo?.updateAvatarUrl(userId, base64)
                android.util.Log.d("OSA_AVATAR", "updateAvatarUrl result: $result")
                settingsRepository.setAvatarUri(base64)
                _avatarBitmap.value = base64ToBitmap(base64)
                _avatarUri.value = null
            } catch (e: Exception) {
                android.util.Log.e("OSA_AVATAR", "Exception: ${e.message}")
                settingsRepository.setAvatarUri(uri.toString())
            }
        }
    }

    fun removeAvatar() {
        _avatarUri.value = null
        _avatarBitmap.value = null
        viewModelScope.launch {
            settingsRepository.setAvatarUri("")
            val userId = sessionManager.getUserId() ?: return@launch
            (supabaseRepo ?: userRepository as? SupabaseUserRepository)?.updateAvatarUrl(userId, null)
        }
    }

    private fun base64ToBitmap(base64: String): Bitmap? = try {
        val pure = base64.substringAfter("base64,")
        val bytes = android.util.Base64.decode(pure, android.util.Base64.DEFAULT)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    } catch (e: Exception) {
        null
    }
}
