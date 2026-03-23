package com.example.asa.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.asa.model.User
import com.example.asa.repository.UserRepository
import com.example.asa.session.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val userRepository: UserRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user.asStateFlow()

    private val _avatarUri = MutableStateFlow<Uri?>(null)
    val avatarUri: StateFlow<Uri?> = _avatarUri.asStateFlow()

    init {
        // Сразу показываем кэш, потом пробуем обновить из сети
        _user.value = sessionManager.getUserCache()
        loadUser()
    }

    fun loadUser() {
        viewModelScope.launch {
            val userId = sessionManager.getUserId() ?: return@launch
            userRepository.getUserById(userId).onSuccess { user ->
                _user.value = user
                sessionManager.saveUserCache(user)
            }
            // При ошибке сети — кэш уже показан, ничего не делаем
        }
    }

    fun updateAvatar(uri: Uri) {
        _avatarUri.value = uri
    }
}
