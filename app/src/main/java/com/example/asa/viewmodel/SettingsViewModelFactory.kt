package com.example.asa.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.asa.repository.SettingsRepository
import com.example.asa.session.SessionManager

class SettingsViewModelFactory(
    private val settingsRepository: SettingsRepository,
    private val sessionManager: SessionManager,
    private val application: Application? = null
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            "Unknown ViewModel class: ${modelClass.name}"
        }
        return SettingsViewModel(settingsRepository, sessionManager, application) as T
    }
}
