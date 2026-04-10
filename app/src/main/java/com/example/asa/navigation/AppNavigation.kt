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
import com.example.asa.ui.splash.SplashScreen
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
    val startDestination = if (sessionManager.isLoggedIn()) "main" else "splash"

    // Проверка версии
    var versionChecked by remember { mutableStateOf(false) }
    var versionSupported by remember { mutableStateOf(true) }
    var retryKey by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(retryKey) {
        versionChecked = false
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
        UpdateRequiredScreen(onRetry = { retryKey++ })
        return
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable("splash") {
            SplashScreen(onFinished = {
                navController.navigate("auth") {
                    popUpTo("splash") { inclusive = true }
                }
            })
        }
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
