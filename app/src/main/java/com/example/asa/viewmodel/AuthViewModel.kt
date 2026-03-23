package com.example.asa.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.asa.model.AuthUiState
import com.example.asa.repository.UserRepository
import com.example.asa.session.SessionManager
import com.example.asa.util.PasswordHasher
import com.example.asa.util.validateLoginForm
import com.example.asa.util.validateRegistrationForm
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(
    private val userRepository: UserRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun register(login: String, password: String, nickname: String, phone: String) {
        val validation = validateRegistrationForm(login, password, nickname, phone)
        if (validation.isFailure) {
            _uiState.value = AuthUiState.Error(validation.exceptionOrNull()?.message ?: "Ошибка валидации")
            return
        }
        _uiState.value = AuthUiState.Loading
        val hash = PasswordHasher.hash(password)
        viewModelScope.launch {
            userRepository.register(login, hash, nickname, phone)
                .onSuccess { user ->
                    sessionManager.saveUserId(user.id)
                    _uiState.value = AuthUiState.Success
                }
                .onFailure { e ->
                    _uiState.value = AuthUiState.Error(e.message ?: "Ошибка регистрации")
                }
        }
    }

    fun login(phone: String, password: String) {
        val validation = validateLoginForm(phone, password)
        if (validation.isFailure) {
            _uiState.value = AuthUiState.Error(validation.exceptionOrNull()?.message ?: "Ошибка валидации")
            return
        }
        _uiState.value = AuthUiState.Loading
        val hash = PasswordHasher.hash(password)
        viewModelScope.launch {
            userRepository.login(phone, hash)
                .onSuccess { user ->
                    sessionManager.saveUserId(user.id)
                    _uiState.value = AuthUiState.Success
                }
                .onFailure { e ->
                    _uiState.value = AuthUiState.Error(e.message ?: "Ошибка входа")
                }
        }
    }

    fun resetState() {
        _uiState.value = AuthUiState.Idle
    }
}
