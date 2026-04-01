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
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
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
    // Единый SettingsViewModel — чтобы smsPhone не сбрасывался при переходах
    val settingsViewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModelFactory(
            settingsRepository,
            sessionManager,
            context.applicationContext as android.app.Application
        )
    )
    val smsPhoneGlobal by settingsViewModel.smsPhone.collectAsState()
    val phone = smsPhoneGlobal

    // Загрузка SMS при старте или смене номера — каждый номер хранит свой кэш
    LaunchedEffect(phone) {
        val readGranted = android.content.pm.PackageManager.PERMISSION_GRANTED ==
                ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_SMS)
        if (readGranted) {
            historyViewModel.setPermissionGranted(true)
            historyViewModel.loadMessages(context, phone)
        }
    }

    // Receiver живёт на уровне MainScreen — слушает входящие SMS всегда, независимо от вкладки
    DisposableEffect(phone) {
        val receiver = SmsReceiver(phone) { body, date ->
            historyViewModel.addIncomingMessage(body, date)
        }
        val filter = IntentFilter(Telephony.Sms.Intents.SMS_RECEIVED_ACTION).apply {
            priority = 999
        }
        context.registerReceiver(receiver, filter)
        onDispose { context.unregisterReceiver(receiver) }
    }

    Scaffold(
        bottomBar = {}
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            NavHost(
                navController = navController,
                startDestination = "chat",
                modifier = Modifier.padding(innerPadding).padding(bottom = 80.dp)
            ) {
                composable("chat") {
                    val chatViewModel: ChatViewModel = viewModel(
                        factory = ChatViewModelFactory(
                            DataStoreSettingsRepository(context),
                            SupabaseUserRepository(),
                            sessionManager
                        )
                    )
                    val currentTheme by settingsRepository.themeFlow.collectAsState(initial = com.example.asa.model.AppTheme.SYSTEM)
                    ChatScreen(
                        viewModel = chatViewModel,
                        smsPhone = phone,
                        isOsaTheme = currentTheme == com.example.asa.model.AppTheme.YELLOW,
                        onNavigateToHistory = { ai, smsText ->
                            historyViewModel.selectAi(ai)
                            historyViewModel.addOutgoingMessage(smsText)
                            navController.navigate("history") {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
                composable("history") {
                    val currentTheme by settingsRepository.themeFlow.collectAsState(initial = com.example.asa.model.AppTheme.SYSTEM)
                    HistoryScreen(
                        viewModel = historyViewModel,
                        smsPhone = phone,
                        isOsaTheme = currentTheme == com.example.asa.model.AppTheme.YELLOW
                    )
                }
                composable("profile") {
                    val profileViewModel: ProfileViewModel = viewModel(
                        factory = ProfileViewModelFactory(
                            SupabaseUserRepository(),
                            sessionManager,
                            settingsRepository
                        )
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
                } // конец composable("profile")
            } // конец NavHost

            // Плавающая навигация с закруглёнными углами
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 10.dp)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(28.dp)
                    )
                    .clip(RoundedCornerShape(28.dp)),
                shape = RoundedCornerShape(28.dp),
                tonalElevation = 6.dp,
                shadowElevation = 12.dp
            ) {
                NavigationBar(
                    modifier = Modifier.clip(RoundedCornerShape(28.dp)),
                    tonalElevation = 0.dp
                ) {
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
        }
    }
}

