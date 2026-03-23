package com.example.asa.ui.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.asa.model.AiAssistant
import com.example.asa.viewmodel.ChatViewModel

private val AI_ICONS = mapOf(
    AiAssistant.CHATGPT to "🤖",
    AiAssistant.DEEPSEEK to "🔍",
    AiAssistant.QWEN to "🌐",
    AiAssistant.PERPLEXITY to "✨"
)

@Composable
fun ChatScreen(viewModel: ChatViewModel, smsPhone: String) {
    val selectedAi by viewModel.selectedAi.collectAsState()
    val inputText by viewModel.inputText.collectAsState()
    val context = LocalContext.current

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
            // Заголовок
            Text(
                text = "AI Ассистент",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            // Секция выбора ассистента
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Выберите ассистента",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val assistants = AiAssistant.entries
                // Сетка 2×2
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    assistants.chunked(2).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            row.forEach { ai ->
                                val icon = AI_ICONS[ai] ?: ""
                                val label = "$icon ${ai.displayName}"
                                if (ai == selectedAi) {
                                    Button(
                                        onClick = { viewModel.selectAi(ai) },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(text = label)
                                    }
                                } else {
                                    OutlinedButton(
                                        onClick = { viewModel.selectAi(ai) },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(text = label)
                                    }
                                }
                            }
                            // Если нечётная строка — добавить пустой вес
                            if (row.size < 2) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            // Поле ввода запроса
            OutlinedTextField(
                value = inputText,
                onValueChange = { viewModel.updateInput(it) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Введите запрос") },
                placeholder = { Text("Напишите что-нибудь...") },
                minLines = 3
            )

            // Кнопка отправки
            Button(
                onClick = { viewModel.sendRequest(context, smsPhone) },
                enabled = inputText.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Отправить")
            }
        }
    }
}
