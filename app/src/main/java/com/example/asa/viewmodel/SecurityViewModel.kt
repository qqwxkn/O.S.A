package com.example.asa.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.asa.model.User
import com.example.asa.repository.UserRepository
import com.example.asa.session.SessionManager
import com.example.asa.util.PasswordHasher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class SecurityUiState {
    object Idle : SecurityUiState()
    object Loading : SecurityUiState()
    data class Error(val message: String) : SecurityUiState()
    object Success : SecurityUiState()
}

class SecurityViewModel(
    private val userRepository: UserRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<SecurityUiState>(SecurityUiState.Idle)
    val uiState: StateFlow<SecurityUiState> = _uiState.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    init {
        loadCurrentUser()
    }

    private fun loadCurrentUser() {
        viewModelScope.launch {
            val userId = sessionManager.getUserId() ?: return@launch
            userRepository.getUserById(userId).onSuccess { user ->
                _currentUser.value = user
            }
        }
    }

    fun reloadUser() {
        loadCurrentUser()
    }

    fun saveChanges(login: String, nickname: String, password: String, vkId: String) {
        if (login.isBlank() || nickname.isBlank()) {
            _uiState.value = SecurityUiState.Error("Поле не может быть пустым")
            return
        }
        _uiState.value = SecurityUiState.Loading
        viewModelScope.launch {
            try {
                val passwordHash = if (password.isNotBlank()) PasswordHasher.hash(password) else null
                val vkIdValue = if (vkId.isNotBlank()) vkId else null
                userRepository.updateUser(
                    id = sessionManager.getUserId() ?: return@launch,
                    login = login,
                    nickname = nickname,
                    passwordHash = passwordHash,
                    vkId = vkIdValue
                ).fold(
                    onSuccess = {
                        loadCurrentUser()
                        _uiState.value = SecurityUiState.Success
                    },
                    onFailure = { e -> _uiState.value = SecurityUiState.Error(e.message ?: "Ошибка сохранения") }
                )
            } catch (e: Exception) {
                _uiState.value = SecurityUiState.Error(e.message ?: "Ошибка сохранения")
            }
        }
    }

    fun resetState() {
        _uiState.value = SecurityUiState.Idle
    }
}
