package com.example.asa.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.asa.repository.SettingsRepository
import com.example.asa.repository.UserRepository
import com.example.asa.session.SessionManager

class ChatViewModelFactory(
    private val settingsRepository: SettingsRepository,
    private val userRepository: UserRepository,
    private val sessionManager: SessionManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return ChatViewModel(settingsRepository, userRepository, sessionManager) as T
    }
}
