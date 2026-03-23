package com.example.asa.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.asa.repository.UserRepository
import com.example.asa.session.SessionManager

class SecurityViewModelFactory(
    private val userRepository: UserRepository,
    private val sessionManager: SessionManager
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(SecurityViewModel::class.java)) {
            "Unknown ViewModel class: ${modelClass.name}"
        }
        return SecurityViewModel(userRepository, sessionManager) as T
    }
}
