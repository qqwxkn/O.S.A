package com.example.asa.navigation

import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.asa.repository.AppConfigRepository
import com.example.asa.repository.SettingsRepository
import com.example.asa.repository.SupabaseUserRepository
import com.example.asa.session.SessionManager
import com.example.asa.ui.auth.AuthScreen
import com.example.asa.ui.update.UpdateRequiredScreen
import com.example.asa.viewmodel.AuthViewModel
import com.example.asa.viewmodel.AuthViewModelFactory
import kotlinx.coroutines.launch

@Composable
fun AppNavigation(
    sessionManager: SessionManager,
    settingsRepository: SettingsRepository
) {
    val navController = rememberNavController()
    val startDestination = if (sessionManager.isLoggedIn()) "main" else "auth"

    // Проверка версии
    var versionChecked by remember { mutableStateOf(false) }
    var versionSupported by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        scope.launch {
            val repo = AppConfigRepository()
            repo.getMinVersion().onSuccess { minVersion ->
                versionSupported = repo.isVersionSupported(minVersion)
            }
            // Если ошибка сети — пускаем (офлайн-режим)
            versionChecked = true
        }
    }

    if (!versionChecked) return // ждём проверки (мгновенно при офлайне)

    if (!versionSupported) {
        UpdateRequiredScreen()
        return
    }

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
