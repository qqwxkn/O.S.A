package com.example.asa.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.asa.model.User
import com.example.asa.repository.SettingsRepository
import com.example.asa.repository.UserRepository
import com.example.asa.session.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val userRepository: UserRepository,
    private val sessionManager: SessionManager,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user.asStateFlow()

    private val _avatarUri = MutableStateFlow<Uri?>(null)
    val avatarUri: StateFlow<Uri?> = _avatarUri.asStateFlow()

    init {
        _user.value = sessionManager.getUserCache()
        loadUser()
        // Загружаем сохранённый URI аватарки
        viewModelScope.launch {
            settingsRepository.avatarUriFlow.collect { uriString ->
                _avatarUri.value = uriString?.let { Uri.parse(it) }
            }
        }
    }

    fun loadUser() {
        viewModelScope.launch {
            val userId = sessionManager.getUserId() ?: return@launch
            userRepository.getUserById(userId).onSuccess { user ->
                _user.value = user
                sessionManager.saveUserCache(user)
            }
        }
    }

    fun updateAvatar(uri: Uri) {
        _avatarUri.value = uri
        viewModelScope.launch {
            settingsRepository.setAvatarUri(uri.toString())
        }
    }

    fun removeAvatar() {
        _avatarUri.value = null
        viewModelScope.launch {
            settingsRepository.setAvatarUri("")
        }
    }
}
