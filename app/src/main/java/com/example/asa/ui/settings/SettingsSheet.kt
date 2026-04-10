package com.example.asa.ui.settings

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.asa.model.AiAssistant
import com.example.asa.model.AppTheme
import com.example.asa.util.isNetworkAvailable
import com.example.asa.viewmodel.SecurityUiState
import com.example.asa.viewmodel.SecurityViewModel
import com.example.asa.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsSheet(
    viewModel: SettingsViewModel,
    securityViewModel: SecurityViewModel,
    onLogout: () -> Unit,
    onDismiss: () -> Unit
) {
    val theme by viewModel.theme.collectAsState()
    val defaultAi by viewModel.defaultAi.collectAsState()
    val smsPhone by viewModel.smsPhone.collectAsState()
    val availablePhones by viewModel.availablePhones.collectAsState()
    val refreshStatus by viewModel.refreshStatus.collectAsState()

    val uiState by securityViewModel.uiState.collectAsState()
    val currentUser by securityViewModel.currentUser.collectAsState()
    val context = LocalContext.current
    val isOnline = remember { isNetworkAvailable(context) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Security form state
    var login by remember { mutableStateOf("") }
    var nickname by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var vkId by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var initialized by remember { mutableStateOf(false) }
    var passwordIsPlaceholder by remember { mutableStateOf(false) }

    LaunchedEffect(currentUser) {
        if (!initialized && currentUser != null) {
            login = currentUser!!.login
            nickname = currentUser!!.nickname
            vkId = currentUser!!.vkId ?: ""
            if (currentUser!!.hasPassword) {
                password = "••••••••"
                passwordIsPlaceholder = true
            }
            initialized = true
        }
    }

    LaunchedEffect(refreshStatus) {
        val msg = refreshStatus ?: return@LaunchedEffect
        scope.launch { snackbarHostState.showSnackbar(msg) }
        viewModel.clearRefreshStatus()
    }

    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is SecurityUiState.Error -> {
                scope.launch { snackbarHostState.showSnackbar(state.message) }
                securityViewModel.resetState()
            }
            is SecurityUiState.Success -> {
                scope.launch { snackbarHostState.showSnackbar("Данные сохранены") }
                securityViewModel.resetState()
            }
            else -> Unit
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Настройки") },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 12.dp)
                .imePadding()
                .verticalScroll(rememberScrollState())
        ) {
            // Обводка вокруг всего контента
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {

            // Тема
            Text("Тема", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(vertical = 4.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    AppTheme.DARK to "Тёмная",
                    AppTheme.LIGHT to "Светлая",
                    AppTheme.SYSTEM to "Системная",
                    AppTheme.YELLOW to "OSA"
                ).forEach { (value, label) ->
                    FilterChip(selected = theme == value, onClick = { viewModel.setTheme(value) }, label = { Text(label, style = MaterialTheme.typography.labelSmall) })
                }
            }

            HorizontalDivider()

            // AI по умолчанию
            Text("AI по умолчанию", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(vertical = 4.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                AiAssistant.entries.forEach { ai ->
                    FilterChip(selected = defaultAi == ai, onClick = { viewModel.setDefaultAi(ai) }, label = { Text(ai.displayName, style = MaterialTheme.typography.labelSmall) })
                }
            }

            HorizontalDivider()

            // Номер для SMS
            Text("Номер для SMS", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(vertical = 4.dp))
            if (availablePhones.isEmpty()) {
                Text("Загрузка номеров...", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 8.dp, bottom = 4.dp))
            } else {
                availablePhones.forEach { (phone, label) ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        RadioButton(selected = smsPhone == phone, onClick = { viewModel.setSmsPhone(phone) })
                        Text(text = label, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            HorizontalDivider()

            // Безопасность
            Text("Безопасность", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(vertical = 4.dp))
            if (!isOnline) {
                Text(
                    text = "Изменить данные можно только при наличии интернета",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            } else {
                Text(
                    text = "Изменения сохраняются только при наличии интернета",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                OutlinedTextField(value = login, onValueChange = { login = it }, label = { Text("Логин", style = MaterialTheme.typography.labelSmall) }, modifier = Modifier.fillMaxWidth(), singleLine = true, textStyle = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(value = nickname, onValueChange = { nickname = it }, label = { Text("Никнейм", style = MaterialTheme.typography.labelSmall) }, modifier = Modifier.fillMaxWidth(), singleLine = true, textStyle = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        if (passwordIsPlaceholder) { passwordIsPlaceholder = false; password = "" } else password = it
                    },
                    label = { Text("Пароль", style = MaterialTheme.typography.labelSmall) },
                    placeholder = { Text("Оставьте пустым, чтобы не менять", style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodySmall,
                    visualTransformation = if (passwordVisible || passwordIsPlaceholder) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = null)
                        }
                    }
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(value = vkId, onValueChange = { vkId = it }, label = { Text("VK ID", style = MaterialTheme.typography.labelSmall) }, modifier = Modifier.fillMaxWidth(), singleLine = true, textStyle = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        val passwordToSave = if (passwordIsPlaceholder) "" else password
                        securityViewModel.saveChanges(login, nickname, passwordToSave, vkId)
                    },
                    enabled = uiState !is SecurityUiState.Loading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (uiState is SecurityUiState.Loading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text("Сохранить")
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))

            TextButton(onClick = { viewModel.logout(); onLogout() }, modifier = Modifier.fillMaxWidth()) {
                Text(text = "Выйти", color = MaterialTheme.colorScheme.error)
            }
            } // end border Column
        }
    }
}
