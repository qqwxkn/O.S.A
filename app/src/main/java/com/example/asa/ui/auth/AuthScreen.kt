package com.example.asa.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.window.Dialog
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import com.example.asa.model.AuthUiState
import com.example.asa.util.normalizePhoneInput
import com.example.asa.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(
    viewModel: AuthViewModel,
    onSuccess: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Запрашиваем все необходимые разрешения при первом открытии
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* результат не важен — просто запросили */ }

    val prefs = remember { context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE) }
    val permRationaleShown = remember { prefs.getBoolean("perm_rationale_shown", false) }
    var showPermRationale by remember { mutableStateOf(!permRationaleShown) }

    if (showPermRationale) {
        Dialog(onDismissRequest = {}) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.5.dp,
                        color = Color(0xFFCCA000),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .background(Color(0xFF0D0D0D), RoundedCornerShape(16.dp))
                    .padding(24.dp)
            ) {
                Column {
                    Text(
                        text = "Разрешения приложения",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Приложению необходим доступ к SMS для:\n\n" +
                            "• Отправки запросов к AI-ассистентам через SMS\n" +
                            "• Получения ответов от AI-ассистентов\n" +
                            "• Отображения истории переписки\n\n" +
                            "Также запрашивается доступ к фото для установки аватара профиля.\n\n" +
                            "Без этих разрешений основные функции приложения будут недоступны.",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = {
                            showPermRationale = false
                            prefs.edit().putBoolean("perm_rationale_shown", true).apply()
                            permissionLauncher.launch(arrayOf(
                                Manifest.permission.SEND_SMS,
                                Manifest.permission.READ_SMS,
                                Manifest.permission.RECEIVE_SMS,
                                Manifest.permission.READ_MEDIA_IMAGES
                            ))
                        },
                        modifier = Modifier.align(Alignment.End),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFCCA000),
                            contentColor = Color(0xFF0D0D0D)
                        )
                    ) {
                        Text("Далее", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        return
    }

    LaunchedEffect(Unit) {
        if (permRationaleShown) {
            permissionLauncher.launch(arrayOf(
                Manifest.permission.SEND_SMS,
                Manifest.permission.READ_SMS,
                Manifest.permission.RECEIVE_SMS,
                Manifest.permission.READ_MEDIA_IMAGES
            ))
        }
    }

    var selectedTab by remember { mutableIntStateOf(0) }

    var loginPhone by remember { mutableStateOf(TextFieldValue("")) }
    var loginPassword by remember { mutableStateOf("") }

    var regLogin by remember { mutableStateOf("") }
    var regPassword by remember { mutableStateOf("") }
    var regNickname by remember { mutableStateOf("") }
    var regPhone by remember { mutableStateOf(TextFieldValue("")) }

    val isLoading = uiState is AuthUiState.Loading

    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is AuthUiState.Success -> onSuccess()
            is AuthUiState.Registered -> {
                selectedTab = 0
                scope.launch {
                    snackbarHostState.showSnackbar("Регистрация успешна! Войдите в аккаунт.")
                    viewModel.resetState()
                }
            }
            is AuthUiState.Error -> {
                scope.launch {
                    snackbarHostState.showSnackbar(state.message)
                    viewModel.resetState()
                }
            }
            else -> Unit
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            Text(text = "💬", fontSize = 56.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "OSA Chat",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "AI-ассистент в вашем кармане",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    TabRow(selectedTabIndex = selectedTab) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("Вход") }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("Регистрация") }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (selectedTab == 0) {
                        LoginForm(
                            phone = loginPhone,
                            password = loginPassword,
                            isLoading = isLoading,
                            onPhoneChange = { tfv ->
                                val normalized = normalizePhoneInput(tfv.text)
                                val addedPrefix = normalized.length > tfv.text.length
                                val cursor = if (addedPrefix) normalized.length else minOf(tfv.selection.end + (normalized.length - tfv.text.length), normalized.length)
                                loginPhone = TextFieldValue(normalized, TextRange(cursor))
                            },
                            onPasswordChange = { loginPassword = it },
                            onLogin = { viewModel.login(loginPhone.text, loginPassword) }
                        )
                    } else {
                        RegisterForm(
                            login = regLogin,
                            password = regPassword,
                            nickname = regNickname,
                            phone = regPhone,
                            isLoading = isLoading,
                            onLoginChange = { regLogin = it },
                            onPasswordChange = { regPassword = it },
                            onNicknameChange = { regNickname = it },
                            onPhoneChange = { tfv ->
                                val normalized = normalizePhoneInput(tfv.text)
                                val addedPrefix = normalized.length > tfv.text.length
                                val cursor = if (addedPrefix) normalized.length else minOf(tfv.selection.end + (normalized.length - tfv.text.length), normalized.length)
                                regPhone = TextFieldValue(normalized, TextRange(cursor))
                            },
                            onRegister = {
                                viewModel.register(regLogin, regPassword, regNickname, regPhone.text)
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun LoginForm(
    phone: TextFieldValue,
    password: String,
    isLoading: Boolean,
    onPhoneChange: (TextFieldValue) -> Unit,
    onPasswordChange: (String) -> Unit,
    onLogin: () -> Unit
) {
    OutlinedTextField(
        value = phone,
        onValueChange = onPhoneChange,
        label = { Text("Номер телефона") },
        placeholder = { Text("89001234567") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        enabled = !isLoading
    )

    Spacer(modifier = Modifier.height(12.dp))

    OutlinedTextField(
        value = password,
        onValueChange = onPasswordChange,
        label = { Text("Пароль") },
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        enabled = !isLoading
    )

    Spacer(modifier = Modifier.height(20.dp))

    Button(
        onClick = onLogin,
        enabled = !isLoading,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(50.dp)),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Black,
            contentColor = Color.White,
            disabledContainerColor = Color.Black.copy(alpha = 0.5f),
            disabledContentColor = Color.White.copy(alpha = 0.5f)
        )
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary
            )
        } else {
            Text("Войти")
        }
    }
}

@Composable
private fun RegisterForm(
    login: String,
    password: String,
    nickname: String,
    phone: TextFieldValue,
    isLoading: Boolean,
    onLoginChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onNicknameChange: (String) -> Unit,
    onPhoneChange: (TextFieldValue) -> Unit,
    onRegister: () -> Unit
) {
    OutlinedTextField(
        value = login,
        onValueChange = onLoginChange,
        label = { Text("Логин") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        enabled = !isLoading
    )

    Spacer(modifier = Modifier.height(12.dp))

    OutlinedTextField(
        value = password,
        onValueChange = onPasswordChange,
        label = { Text("Пароль") },
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        enabled = !isLoading
    )

    Spacer(modifier = Modifier.height(12.dp))

    OutlinedTextField(
        value = nickname,
        onValueChange = onNicknameChange,
        label = { Text("Никнейм") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        enabled = !isLoading
    )

    Spacer(modifier = Modifier.height(12.dp))

    OutlinedTextField(
        value = phone,
        onValueChange = onPhoneChange,
        label = { Text("Номер телефона") },
        placeholder = { Text("89001234567") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        enabled = !isLoading
    )

    Spacer(modifier = Modifier.height(20.dp))

    Button(
        onClick = onRegister,
        enabled = !isLoading,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(50.dp)),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Black,
            contentColor = Color.White,
            disabledContainerColor = Color.Black.copy(alpha = 0.5f),
            disabledContentColor = Color.White.copy(alpha = 0.5f)
        )
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary
            )
        } else {
            Text("Зарегистрироваться")
        }
    }
}
