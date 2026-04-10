package com.example.asa.navigation

import android.content.IntentFilter
import android.provider.Telephony
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.example.asa.ui.chat.ChatScreen
import com.example.asa.ui.history.HistoryScreen
import com.example.asa.ui.profile.ProfileScreen
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
    BottomTab("vkchat", "ВК Чат", Icons.AutoMirrored.Filled.Message),
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

    var showSettings by remember { mutableStateOf(false) }
    var showCropper by remember { mutableStateOf(false) }

    val historyViewModel: HistoryViewModel = viewModel(factory = HistoryViewModelFactory())
    val settingsViewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModelFactory(
            settingsRepository,
            sessionManager,
            context.applicationContext as android.app.Application
        )
    )
    val securityViewModel: SecurityViewModel = viewModel(
        factory = SecurityViewModelFactory(SupabaseUserRepository(), sessionManager)
    )

    val smsPhoneGlobal by settingsViewModel.smsPhone.collectAsState()
    val phone = smsPhoneGlobal

    LaunchedEffect(phone) {
        val readGranted = android.content.pm.PackageManager.PERMISSION_GRANTED ==
                ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_SMS)
        if (readGranted) {
            historyViewModel.setPermissionGranted(true)
            historyViewModel.loadMessages(context, phone)
        }
    }

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

    if (showSettings) {
        BackHandler { showSettings = false }
        SettingsSheet(
            viewModel = settingsViewModel,
            securityViewModel = securityViewModel,
            onLogout = onLogout,
            onDismiss = { showSettings = false }
        )
        return
    }

    Scaffold(
        bottomBar = {},
        contentWindowInsets = WindowInsets(0)
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            val imeBottom = WindowInsets.ime.getBottom(LocalDensity.current)
            val isKeyboardVisible = imeBottom > 100

            NavHost(
                navController = navController,
                startDestination = "chat",
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(bottom = 80.dp)
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
                    val availablePhones by settingsViewModel.availablePhones.collectAsState()
                    HistoryScreen(
                        viewModel = historyViewModel,
                        smsPhone = phone,
                        onRefreshPhone = { settingsViewModel.refreshPhones() },
                        isOsaTheme = currentTheme == com.example.asa.model.AppTheme.YELLOW
                    )
                }
                composable("vkchat") {
                    androidx.compose.foundation.layout.Box(
                        modifier = androidx.compose.ui.Modifier.fillMaxSize(),
                        contentAlignment = androidx.compose.ui.Alignment.Center
                    ) {
                        androidx.compose.material3.Text(
                            text = "ВК Чат\nСкоро",
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                composable("profile") {
                    val profileViewModel: ProfileViewModel = viewModel(
                        factory = ProfileViewModelFactory(
                            SupabaseUserRepository(),
                            sessionManager,
                            settingsRepository,
                            context
                        )
                    )
                    ProfileScreen(
                        viewModel = profileViewModel,
                        onOpenSettings = { showSettings = true },
                        onShowCropper = { showCropper = true },
                        onHideCropper = { showCropper = false }
                    )
                }
            }

            AnimatedVisibility(
                visible = !showCropper,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Surface(
                modifier = Modifier
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
                                        popUpTo(navController.graph.startDestinationId) { saveState = true }
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
            } // end Surface
            } // end AnimatedVisibility
        }
    }
}
