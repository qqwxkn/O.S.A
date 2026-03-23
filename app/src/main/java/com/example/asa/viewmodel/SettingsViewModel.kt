package com.example.asa.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.asa.model.AiAssistant
import com.example.asa.model.AppTheme
import com.example.asa.repository.SettingsRepository
import com.example.asa.session.SessionManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    val theme: StateFlow<AppTheme> = settingsRepository.themeFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppTheme.SYSTEM)

    val defaultAi: StateFlow<AiAssistant> = settingsRepository.defaultAiFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, AiAssistant.CHATGPT)

    val smsPhone: StateFlow<String> = settingsRepository.smsPhoneFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, "89155399434")

    fun setTheme(theme: AppTheme) {
        viewModelScope.launch { settingsRepository.setTheme(theme) }
    }

    fun setDefaultAi(ai: AiAssistant) {
        viewModelScope.launch { settingsRepository.setDefaultAi(ai) }
    }

    fun setSmsPhone(phone: String) {
        viewModelScope.launch { settingsRepository.setSmsPhone(phone) }
    }

    fun logout() {
        sessionManager.clearSession()
    }
}
