package com.example.asa.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.asa.repository.SettingsRepository
import com.example.asa.repository.SupabaseUserRepository
import com.example.asa.session.SessionManager
import com.example.asa.ui.auth.AuthScreen
import com.example.asa.viewmodel.AuthViewModel
import com.example.asa.viewmodel.AuthViewModelFactory

@Composable
fun AppNavigation(
    sessionManager: SessionManager,
    settingsRepository: SettingsRepository
) {
    val navController = rememberNavController()
    val startDestination = if (sessionManager.isLoggedIn()) "main" else "auth"

    NavHost(navController = navController, startDestination = startDestination) {
        composable("auth") {
            val authViewModel: AuthViewModel = viewModel(
                factory = AuthViewModelFactory(
                    userRepository = SupabaseUserRepository(),
                    sessionManager = sessionManager
                )
            )
            AuthScreen(
                viewModel = authViewModel,
                onSuccess = {
                    navController.navigate("main") {
                        popUpTo("auth") { inclusive = true }
                    }
                }
            )
        }
        composable("main") {
            MainScreen(
                sessionManager = sessionManager,
                settingsRepository = settingsRepository,
                onLogout = {
                    navController.navigate("auth") {
                        popUpTo("main") { inclusive = true }
                    }
                }
            )
        }
    }
}
