package com.example.asa.navigation

import android.content.IntentFilter
import android.provider.Telephony
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.asa.model.AiAssistant
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.asa.receiver.SmsReceiver
import com.example.asa.repository.DataStoreSettingsRepository
import com.example.asa.repository.SettingsRepository
import com.example.asa.repository.SupabaseUserRepository
import com.example.asa.session.SessionManager
import com.example.asa.ui.history.HistoryScreen
import com.example.asa.ui.chat.ChatScreen
import com.example.asa.ui.profile.ProfileScreen
import com.example.asa.ui.settings.SecuritySheet
import com.example.asa.ui.settings.SettingsSheet
import com.example.asa.viewmodel.ChatViewModel
import com.example.asa.viewmodel.ChatViewModelFactory
import com.example.asa.viewmodel.HistoryViewModel
import com.example.asa.viewmodel.HistoryViewModelFactory
import com.example.asa.viewmodel.ProfileViewModel
import com.example.asa.viewmodel.ProfileViewModelFactory
import com.example.asa.viewmodel.SecurityViewModel
import com.example.asa.viewmodel.SecurityViewModelFactory
import com.example.asa.viewmodel.SettingsViewModel
import com.example.asa.viewmodel.SettingsViewModelFactory

private data class BottomTab(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

private val tabs = listOf(
    BottomTab("chat", "Чаты", Icons.Default.Forum),
    BottomTab("history", "История", Icons.Default.History),
    BottomTab("profile", "Профиль", Icons.Default.Person)
)

@Composable
fun MainScreen(
    sessionManager: SessionManager,
    settingsRepository: SettingsRepository,
    onLogout: () -> Unit
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val context = LocalContext.current

    // Единый HistoryViewModel для всего экрана — чтобы ChatScreen мог переключить AI
    val historyViewModel: HistoryViewModel = viewModel(factory = HistoryViewModelFactory())
    val smsPhoneGlobal by settingsRepository.smsPhoneFlow.collectAsState(initial = "89155399434")

    // Загрузка SMS при старте или смене номера — каждый номер хранит свой кэш
    LaunchedEffect(smsPhoneGlobal) {
        val readGranted = android.content.pm.PackageManager.PERMISSION_GRANTED ==
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_SMS)
        if (readGranted) {
            historyViewModel.setPermissionGranted(true)
            historyViewModel.loadMessages(context, smsPhoneGlobal)
        }
    }

    // Receiver живёт на уровне MainScreen — слушает входящие SMS всегда, независимо от вкладки
    DisposableEffect(smsPhoneGlobal) {
        val receiver = SmsReceiver(smsPhoneGlobal) { body, date ->
            historyViewModel.addIncomingMessage(body, date)
        }
        val filter = IntentFilter(Telephony.Sms.Intents.SMS_RECEIVED_ACTION).apply {
            priority = 999
        }
        context.registerReceiver(receiver, filter)
        onDispose { context.unregisterReceiver(receiver) }
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEach { tab ->
                    NavigationBarItem(
                        selected = currentRoute == tab.route,
                        onClick = {
                            if (currentRoute != tab.route) {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.startDestinationId) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = { Icon(imageVector = tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "chat",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("chat") {
                val chatViewModel: ChatViewModel = viewModel(
                    factory = ChatViewModelFactory(
                        DataStoreSettingsRepository(context),
                        SupabaseUserRepository(),
                        sessionManager
                    )
                )
                val smsPhone by settingsRepository.smsPhoneFlow.collectAsState(initial = "89155399434")
                ChatScreen(
                    viewModel = chatViewModel,
                    smsPhone = smsPhone,
                    onNavigateToHistory = { ai, smsText ->
                        historyViewModel.selectAi(ai)
                        historyViewModel.addOutgoingMessage(smsText) // добавляем ДО навигации
                        navController.navigate("history") {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
            composable("history") {
                val smsPhone by settingsRepository.smsPhoneFlow.collectAsState(initial = "89155399434")
                HistoryScreen(viewModel = historyViewModel, smsPhone = smsPhone)
            }
            composable("profile") {
                val profileViewModel: ProfileViewModel = viewModel(
                    factory = ProfileViewModelFactory(SupabaseUserRepository(), sessionManager, settingsRepository)
                )
                val settingsViewModel: SettingsViewModel = viewModel(
                    factory = SettingsViewModelFactory(settingsRepository, sessionManager)
                )
                val securityViewModel: SecurityViewModel = viewModel(
                    factory = SecurityViewModelFactory(SupabaseUserRepository(), sessionManager)
                )

                var showSettingsSheet by remember { mutableStateOf(false) }
                var showSecuritySheet by remember { mutableStateOf(false) }

                ProfileScreen(
                    viewModel = profileViewModel,
                    onOpenSettings = { showSettingsSheet = true }
                )

                if (showSettingsSheet) {
                    SettingsSheet(
                        viewModel = settingsViewModel,
                        onOpenSecurity = {
                            showSecuritySheet = true
                            showSettingsSheet = false
                        },
                        onLogout = onLogout,
                        onDismiss = { showSettingsSheet = false }
                    )
                }

                if (showSecuritySheet) {
                    SecuritySheet(
                        viewModel = securityViewModel,
                        onDismiss = { showSecuritySheet = false }
                    )
                }
            }
        }
    }
}
