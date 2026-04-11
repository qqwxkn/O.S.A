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
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat

@Composable
fun AppNavigation(
    sessionManager: SessionManager,
    settingsRepository: SettingsRepository
) {
    val navController = rememberNavController()
    val startDestination = "permissions"

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
        composable("permissions") {
            PermissionsScreen(
                onGranted = {
                    val next = if (sessionManager.isLoggedIn()) "main" else "splash"
                    navController.navigate(next) {
                        popUpTo("permissions") { inclusive = true }
                    }
                }
            )
        }
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

@Composable
private fun PermissionsScreen(onGranted: () -> Unit) {
    val context = LocalContext.current

    val required = arrayOf(
        Manifest.permission.READ_SMS,
        Manifest.permission.RECEIVE_SMS,
        Manifest.permission.SEND_SMS
    )

    fun allGranted() = required.all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

    var granted by remember { mutableStateOf(allGranted()) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        granted = results.values.all { it }
        if (granted) onGranted()
    }

    LaunchedEffect(Unit) {
        if (granted) {
            onGranted()
        } else {
            launcher.launch(required)
        }
    }

    if (!granted) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("📩", fontSize = 64.sp)
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Для работы приложения необходим доступ к SMS",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Приложение использует SMS для отправки запросов к AI ассистентам и получения ответов.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = { launcher.launch(required) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Разрешить доступ к SMS")
            }
        }
    }
}
