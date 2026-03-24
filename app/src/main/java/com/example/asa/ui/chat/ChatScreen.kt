package com.example.asa.ui.chat

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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
    AiAssistant.PERPLEXITY to "✨"
)

@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    smsPhone: String,
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
                .padding(horizontal = 16.dp)
                .padding(top = 170.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = "AI Ассистент",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                                    Button(
                                        onClick = { viewModel.selectAi(ai) },
                                        modifier = Modifier.weight(1f)
                                    ) { Text(label) }
                                } else {
                                    OutlinedButton(
                                        onClick = { viewModel.selectAi(ai) },
                                        modifier = Modifier.weight(1f)
                                    ) { Text(label) }
                                }
                            }
                            if (row.size < 2) Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            OutlinedTextField(
                value = inputText,
                onValueChange = { viewModel.updateInput(it) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Введите запрос") },
                placeholder = { Text("Напишите что-нибудь...") },
                minLines = 3
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
