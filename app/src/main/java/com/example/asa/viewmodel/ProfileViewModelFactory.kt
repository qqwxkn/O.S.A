package com.example.asa.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.asa.repository.SettingsRepository
import com.example.asa.repository.UserRepository
import com.example.asa.session.SessionManager

class ProfileViewModelFactory(
    private val userRepository: UserRepository,
    private val sessionManager: SessionManager,
    private val settingsRepository: SettingsRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return ProfileViewModel(userRepository, sessionManager, settingsRepository) as T
    }
}
