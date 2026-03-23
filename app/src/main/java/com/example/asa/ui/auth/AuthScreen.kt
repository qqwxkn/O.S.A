package com.example.asa.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.asa.model.AuthUiState
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

    var selectedTab by remember { mutableIntStateOf(0) }

    // Поля входа
    var loginPhone by remember { mutableStateOf("") }
    var loginPassword by remember { mutableStateOf("") }

    // Поля регистрации
    var regLogin by remember { mutableStateOf("") }
    var regPassword by remember { mutableStateOf("") }
    var regNickname by remember { mutableStateOf("") }
    var regPhone by remember { mutableStateOf("") }

    val isLoading = uiState is AuthUiState.Loading

    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is AuthUiState.Success -> onSuccess()
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Spacer(modifier = Modifier.height(32.dp))

                // Логотип / заголовок
                Text(
                    text = "💬",
                    fontSize = 56.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "ASA Chat",
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

                // Карточка с формой
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
                                onPhoneChange = { loginPhone = it },
                                onPasswordChange = { loginPassword = it },
                                onLogin = { viewModel.login(loginPhone, loginPassword) }
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
                                onPhoneChange = { regPhone = it },
                                onRegister = {
                                    viewModel.register(regLogin, regPassword, regNickname, regPhone)
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun LoginForm(
    phone: String,
    password: String,
    isLoading: Boolean,
    onPhoneChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onLogin: () -> Unit
) {
    OutlinedTextField(
        value = phone,
        onValueChange = onPhoneChange,
        label = { Text("Номер телефона") },
        placeholder = { Text("+79001234567") },
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
        modifier = Modifier.fillMaxWidth()
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
    phone: String,
    isLoading: Boolean,
    onLoginChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onNicknameChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
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
        placeholder = { Text("+79001234567") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        enabled = !isLoading
    )

    Spacer(modifier = Modifier.height(20.dp))

    Button(
        onClick = onRegister,
        enabled = !isLoading,
        modifier = Modifier.fillMaxWidth()
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
