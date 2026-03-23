package com.example.asa.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.asa.model.AiAssistant
import com.example.asa.repository.SettingsRepository
import com.example.asa.repository.UserRepository
import com.example.asa.session.SessionManager
import com.example.asa.util.SmsLauncher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChatViewModel(
    private val settingsRepository: SettingsRepository,
    private val userRepository: UserRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _selectedAi = MutableStateFlow<AiAssistant>(AiAssistant.CHATGPT)
    val selectedAi: StateFlow<AiAssistant> = _selectedAi.asStateFlow()

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.defaultAiFlow.collect { ai ->
                _selectedAi.value = ai
            }
        }
    }

    fun selectAi(ai: AiAssistant) {
        _selectedAi.value = ai
    }

    fun updateInput(text: String) {
        _inputText.value = text
    }

    fun sendRequest(context: Context, smsPhone: String) {
        val inputText = _inputText.value
        val ai = _selectedAi.value
        val smsText = "[ ${ai.displayName} ]\n$inputText"
        SmsLauncher.launch(context, smsPhone, smsText)
        viewModelScope.launch {
            userRepository.incrementRequestsCount(sessionManager.getUserId() ?: return@launch)
        }
        _inputText.value = ""
    }
}
