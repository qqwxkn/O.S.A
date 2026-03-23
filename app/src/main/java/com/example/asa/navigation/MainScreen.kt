package com.example.asa.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.asa.repository.DataStoreSettingsRepository
import com.example.asa.repository.SettingsRepository
import com.example.asa.repository.SupabaseUserRepository
import com.example.asa.session.SessionManager
import com.example.asa.ui.chat.ChatScreen
import com.example.asa.ui.profile.ProfileScreen
import com.example.asa.ui.settings.SecuritySheet
import com.example.asa.ui.settings.SettingsSheet
import com.example.asa.viewmodel.ChatViewModel
import com.example.asa.viewmodel.ChatViewModelFactory
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
    BottomTab("profile", "Профиль", Icons.Default.Person),
    BottomTab("placeholder", "Скоро", Icons.Default.Star)
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
                ChatScreen(viewModel = chatViewModel, smsPhone = smsPhone)
            }
            composable("profile") {
                val profileViewModel: ProfileViewModel = viewModel(
                    factory = ProfileViewModelFactory(SupabaseUserRepository(), sessionManager)
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
            composable("placeholder") {
                PlaceholderScreen()
            }
        }
    }
}

@Composable
fun PlaceholderScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Скоро здесь появится что-то интересное")
    }
}
