package com.example.asa.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.asa.model.AiAssistant
import com.example.asa.model.AppTheme
import com.example.asa.repository.SettingsRepository
import com.example.asa.repository.SmsPhoneRepository
import com.example.asa.session.SessionManager
import com.example.asa.util.  isNetworkAvailable
import android.app.Application
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val sessionManager: SessionManager,
    private val application: Application? = null
) : ViewModel() {

    private val smsPhoneRepository = SmsPhoneRepository()

    val theme: StateFlow<AppTheme> = settingsRepository.themeFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppTheme.YELLOW)

    val defaultAi: StateFlow<AiAssistant> = settingsRepository.defaultAiFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, AiAssistant.CHATGPT)

    val smsPhone: StateFlow<String> = settingsRepository.smsPhoneFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    val availablePhones: StateFlow<List<Pair<String, String>>> = settingsRepository.availablePhonesFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _refreshStatus = MutableStateFlow<String?>(null)
    val refreshStatus: StateFlow<String?> = _refreshStatus.asStateFlow()

    init {
        refreshPhonesIfOnline()
    }

    private fun refreshPhonesIfOnline() {
        viewModelScope.launch {
            val context = application ?: return@launch
            if (!isNetworkAvailable(context)) return@launch
            smsPhoneRepository.fetchPhones().onSuccess { phones ->
                if (phones.isNotEmpty()) {
                    settingsRepository.setAvailablePhones(phones)
                    // НЕ меняем текущий выбранный номер автоматически
                }
            }
        }
    }

    fun refreshPhones() {
        viewModelScope.launch {
            val context = application ?: return@launch
            if (!isNetworkAvailable(context)) {
                _refreshStatus.value = "Нет подключения к интернету"
                return@launch
            }
            smsPhoneRepository.fetchPhones()
                .onSuccess { phones ->
                    if (phones.isNotEmpty()) {
                        settingsRepository.setAvailablePhones(phones)
                        _refreshStatus.value = "Номера обновлены"
                    } else {
                        _refreshStatus.value = "Нет доступных номеров"
                    }
                }
                .onFailure {
                    _refreshStatus.value = "Ошибка загрузки: ${it.message}"
                }
        }
    }

    fun clearRefreshStatus() {
        _refreshStatus.value = null
    }

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
