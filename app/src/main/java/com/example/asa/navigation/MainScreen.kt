package com.example.asa.navigation

import android.content.IntentFilter
import android.provider.Telephony
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.asa.receiver.SmsReceiver
import com.example.asa.repository.SettingsRepository
import com.example.asa.repository.SupabaseUserRepository
import com.example.asa.session.SessionManager
import com.example.asa.ui.history.HistoryScreen
import com.example.asa.ui.profile.ProfileScreen
import com.example.asa.ui.settings.SettingsSheet
import com.example.asa.viewmodel.HistoryViewModel
import com.example.asa.viewmodel.HistoryViewModelFactory
import com.example.asa.viewmodel.ProfileViewModel
import com.example.asa.viewmodel.ProfileViewModelFactory
import com.example.asa.viewmodel.SecurityViewModel
import com.example.asa.viewmodel.SecurityViewModelFactory
import com.example.asa.viewmodel.SettingsViewModel
import com.example.asa.viewmodel.SettingsViewModelFactory
import kotlinx.coroutines.launch

private data class Tab(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val tabs = listOf(
    Tab("Чаты", Icons.Default.History),
    Tab("ВК Чат", Icons.AutoMirrored.Filled.Message),
    Tab("Профиль", Icons.Default.Person)
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainScreen(sessionManager: SessionManager, settingsRepository: SettingsRepository, onLogout: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    var showSettings by remember { mutableStateOf(false) }
    var showCropper by remember { mutableStateOf(false) }
    var chatOpen by remember { mutableStateOf(false) }
    val historyViewModel: HistoryViewModel = viewModel(factory = HistoryViewModelFactory())
    val settingsViewModel: SettingsViewModel = viewModel(factory = SettingsViewModelFactory(settingsRepository, sessionManager, context.applicationContext as android.app.Application))
    val securityViewModel: SecurityViewModel = viewModel(factory = SecurityViewModelFactory(SupabaseUserRepository(), sessionManager))
    val phone by settingsViewModel.smsPhone.collectAsState()
    LaunchedEffect(phone) {
        val granted = android.content.pm.PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_SMS)
        if (granted) { historyViewModel.setPermissionGranted(true); historyViewModel.loadMessages(context, phone) }
    }
    DisposableEffect(phone) {
        val receiver = SmsReceiver(phone) { body, date -> historyViewModel.addIncomingMessage(body, date) }
        val filter = IntentFilter(Telephony.Sms.Intents.SMS_RECEIVED_ACTION).apply { priority = 999 }
        context.registerReceiver(receiver, filter)
        onDispose { context.unregisterReceiver(receiver) }
    }
    if (showSettings) {
        BackHandler { showSettings = false }
        SettingsSheet(viewModel = settingsViewModel, securityViewModel = securityViewModel, onLogout = onLogout, onDismiss = { showSettings = false })
        return
    }
    val isKeyboardVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 100
    Scaffold(bottomBar = {}, contentWindowInsets = WindowInsets(0)) { _ ->
        Box(modifier = Modifier.fillMaxSize()) {
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(), userScrollEnabled = !isKeyboardVisible) { page ->
                when (page) {
                    0 -> {
                        val currentTheme by settingsRepository.themeFlow.collectAsState(initial = com.example.asa.model.AppTheme.SYSTEM)
                        val refreshStatus by settingsViewModel.refreshStatus.collectAsState()
                        HistoryScreen(
                            viewModel = historyViewModel,
                            smsPhone = phone,
                            onRefreshPhone = { settingsViewModel.refreshPhones() },
                            refreshStatus = refreshStatus,
                            onClearRefreshStatus = { settingsViewModel.clearRefreshStatus() },
                            isOsaTheme = currentTheme == com.example.asa.model.AppTheme.YELLOW,
                            onChatOpenChanged = { chatOpen = it }
                        )
                    }
                    1 -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = "ВК Чат\nСкоро", textAlign = androidx.compose.ui.text.style.TextAlign.Center, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    2 -> {
                        val profileViewModel: ProfileViewModel = viewModel(factory = ProfileViewModelFactory(SupabaseUserRepository(), sessionManager, settingsRepository, context))
                        ProfileScreen(viewModel = profileViewModel, onOpenSettings = { showSettings = true }, onShowCropper = { showCropper = true }, onHideCropper = { showCropper = false })
                    }
                }
            }
            AnimatedVisibility(
                visible = !showCropper && !isKeyboardVisible && !chatOpen,
                enter = slideInHorizontally(animationSpec = tween(300), initialOffsetX = { it }),
                exit = androidx.compose.animation.ExitTransition.None,
                modifier = Modifier.align(Alignment.BottomEnd).wrapContentWidth()
            ) {
                Surface(modifier = Modifier.wrapContentWidth().navigationBarsPadding().padding(end = 10.dp, bottom = 24.dp).border(1.dp, androidx.compose.ui.graphics.Color(0xFFFFD600).copy(alpha = 0.7f), RoundedCornerShape(28.dp)).clip(RoundedCornerShape(28.dp)), shape = RoundedCornerShape(28.dp), color = androidx.compose.ui.graphics.Color.Black, tonalElevation = 0.dp, shadowElevation = 12.dp) {
                    Column(modifier = Modifier.width(IntrinsicSize.Min).clip(RoundedCornerShape(28.dp)).padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        tabs.forEachIndexed { index, tab ->
                            val selected = pagerState.currentPage == index
                            Column(modifier = Modifier.clip(RoundedCornerShape(20.dp)).then(if (selected) Modifier.background(MaterialTheme.colorScheme.secondaryContainer) else Modifier).clickable { scope.launch { pagerState.animateScrollToPage(index) } }.padding(horizontal = 12.dp, vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Icon(imageVector = tab.icon, contentDescription = tab.label, tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
                                Text(text = tab.label, style = MaterialTheme.typography.labelSmall, color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        // Шестерёнка — только на вкладке Профиль
                        if (pagerState.currentPage == 2) {
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), color = androidx.compose.ui.graphics.Color(0xFFFFD600).copy(alpha = 0.3f))
                            Column(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .clickable { showSettings = true }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Settings, contentDescription = "Настройки", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
                            }
                        }                    }
                }
            }
        }
    }
}
