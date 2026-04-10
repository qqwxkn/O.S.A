package com.example.asa.ui.chat

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.asa.model.AiAssistant
import com.example.asa.viewmodel.ChatViewModel

private val AI_ICONS = mapOf(
    AiAssistant.CHATGPT to "🤖",
    AiAssistant.DEEPSEEK to "🔍",
    AiAssistant.QWEN to "🌐",
    AiAssistant.PERPLEXITY to "✨",
    AiAssistant.CLAUDE to "🧠",
    AiAssistant.GEMINI to "💎"
)

@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    smsPhone: String,
    isOsaTheme: Boolean = false,
    onNavigateToHistory: (AiAssistant, String) -> Unit = { _, _ -> }
) {
    val selectedAi by viewModel.selectedAi.collectAsState()
    val inputText by viewModel.inputText.collectAsState()
    val navigateToHistory by viewModel.navigateToHistory.collectAsState()
    val context = LocalContext.current

    var hasSmsPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasSmsPermission = granted
    }

    // Переход в историю после отправки
    LaunchedEffect(navigateToHistory) {
        val pair = navigateToHistory
        if (pair != null) {
            onNavigateToHistory(pair.first, pair.second)
            viewModel.onNavigatedToHistory()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .padding(horizontal = 16.dp)
                .padding(top = 24.dp, bottom = 16.dp)
        ) {
            // Верхняя часть — скроллируемый выбор ассистента
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "AI Ассистент",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.fillMaxWidth()
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Выберите ассистента",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        AiAssistant.entries.chunked(2).forEach { row ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                row.forEach { ai ->
                                    val label = "${AI_ICONS[ai] ?: ""} ${ai.displayName}"
                                    if (ai == selectedAi) {
                                        if (isOsaTheme) {
                                            Button(
                                                onClick = { viewModel.selectAi(ai) },
                                                modifier = Modifier.weight(1f),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = Color.White,
                                                    contentColor = Color.Black
                                                )
                                            ) { Text(label) }
                                        } else {
                                            Button(
                                                onClick = { viewModel.selectAi(ai) },
                                                modifier = Modifier.weight(1f)
                                            ) { Text(label) }
                                        }
                                    } else {
                                        OutlinedButton(
                                            onClick = { viewModel.selectAi(ai) },
                                            modifier = Modifier.weight(1f),
                                            colors = if (isOsaTheme) ButtonDefaults.outlinedButtonColors(
                                                contentColor = MaterialTheme.colorScheme.onBackground
                                            ) else ButtonDefaults.outlinedButtonColors()
                                        ) { Text(label) }
                                    }
                                }
                                if (row.size < 2) Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            val imeHeight = WindowInsets.ime.getBottom(LocalDensity.current)
            // Нижняя часть — поле ввода и кнопка, поднимаются с клавиатурой
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { viewModel.updateInput(it) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Введите запрос") },
                    placeholder = { Text("Напишите что-нибудь...") },
                    minLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                    )
                )

                Button(
                    onClick = {
                        if (hasSmsPermission) {
                            viewModel.sendRequest(context, smsPhone)
                        } else {
                            permissionLauncher.launch(Manifest.permission.SEND_SMS)
                        }
                    },
                    enabled = inputText.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (hasSmsPermission) "Отправить" else "Разрешить SMS и отправить")
                }
            }
        }
    }
}
