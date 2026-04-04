package com.example.asa.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.asa.repository.SettingsRepository
import com.example.asa.repository.SupabaseUserRepository
import com.example.asa.repository.UserRepository
import com.example.asa.session.SessionManager

class ProfileViewModelFactory(
    private val userRepository: UserRepository,
    private val sessionManager: SessionManager,
    private val settingsRepository: SettingsRepository,
    private val context: Context? = null
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val supabaseRepo = userRepository as? SupabaseUserRepository
        @Suppress("UNCHECKED_CAST")
        return ProfileViewModel(userRepository, supabaseRepo, sessionManager, settingsRepository, context) as T
    }
}
