package com.example.asa.ui.history

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.asa.model.AiAssistant
import com.example.asa.util.SmsLauncher
import com.example.asa.viewmodel.HistoryViewModel
import com.example.asa.viewmodel.SmsMessage
import java.text.SimpleDateFormat
import java.util.*

private val AI_ICONS = mapOf(
    AiAssistant.CHATGPT to "🤖",
    AiAssistant.DEEPSEEK to "🔍",
    AiAssistant.QWEN to "🌐",
    AiAssistant.PERPLEXITY to "✨",
    AiAssistant.CLAUDE to "🧠",
    AiAssistant.GEMINI to "💎"
)

private val AI_COLORS = listOf(
    Color(0xFF5C6BC0), Color(0xFF26A69A), Color(0xFFEF5350),
    Color(0xFFAB47BC), Color(0xFF42A5F5), Color(0xFFFF7043)
)

@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    smsPhone: String,
    onRefreshPhone: () -> Unit = {},
    refreshStatus: String? = null,
    onClearRefreshStatus: () -> Unit = {},
    isOsaTheme: Boolean = false,
    onChatOpenChanged: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val hasPermission by viewModel.hasPermission.collectAsState()
    var openedAi by remember { mutableStateOf<AiAssistant?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val readGranted = permissions[Manifest.permission.READ_SMS] == true
        viewModel.setPermissionGranted(readGranted)
        if (readGranted) viewModel.loadMessages(context, smsPhone)
    }

    LaunchedEffect(Unit) {
        val readGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
        val receiveGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED
        viewModel.setPermissionGranted(readGranted)
        if (!readGranted || !receiveGranted) {
            permissionLauncher.launch(arrayOf(Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS))
        }
    }

    val ai = openedAi
    if (ai != null) {
        ChatDetailScreen(
            viewModel = viewModel,
            smsPhone = smsPhone,
            ai = ai,
            isOsaTheme = isOsaTheme,
            onBack = {
                openedAi = null
                onChatOpenChanged(false)
            }
        )
    } else {
        ChatListScreen(
            viewModel = viewModel,
            smsPhone = smsPhone,
            hasPermission = hasPermission,
            onOpenChat = {
                openedAi = it
                onChatOpenChanged(true)
            },
            onRequestPermission = {
                permissionLauncher.launch(arrayOf(Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS))
            },
            onRefreshPhone = onRefreshPhone,
            refreshStatus = refreshStatus,
            onClearRefreshStatus = onClearRefreshStatus
        )
    }
}

@Composable
private fun ChatListScreen(
    viewModel: HistoryViewModel,
    smsPhone: String,
    hasPermission: Boolean,
    onOpenChat: (AiAssistant) -> Unit,
    onRequestPermission: () -> Unit,
    onRefreshPhone: () -> Unit = {},
    refreshStatus: String? = null,
    onClearRefreshStatus: () -> Unit = {}
) {
    val allMessages by viewModel.allMessages.collectAsState()
    var showToast by remember { mutableStateOf(false) }
    var toastMessage by remember { mutableStateOf("") }

    LaunchedEffect(refreshStatus) {
        if (refreshStatus != null) {
            toastMessage = refreshStatus
            showToast = true
            kotlinx.coroutines.delay(3000)
            showToast = false
            onClearRefreshStatus()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Чаты",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.weight(1f))
                if (smsPhone.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = smsPhone,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }
                IconButton(onClick = { onRefreshPhone() }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Refresh, contentDescription = "Обновить номер", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                }
            }

            HorizontalDivider()

            if (!hasPermission) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("📩", fontSize = 48.sp, modifier = Modifier.padding(bottom = 16.dp))
                    Text(
                        "Для отображения чатов нужен доступ к SMS",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(bottom = 24.dp, start = 32.dp, end = 32.dp)
                    )
                    Button(onClick = onRequestPermission) { Text("Разрешить доступ к SMS") }
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(AiAssistant.entries) { ai ->
                        val prefix = "[ ${ai.displayName} ]"
                        val msgs = allMessages.filter { it.body.contains(prefix) }.sortedBy { it.date }
                        val last = msgs.lastOrNull()
                        AiChatRow(ai = ai, lastMessage = last, onClick = { onOpenChat(ai) })
                        HorizontalDivider(modifier = Modifier.padding(start = 72.dp))
                    }
                }
            }
        }

        // Тост сверху
        androidx.compose.animation.AnimatedVisibility(
            visible = showToast,
            enter = androidx.compose.animation.slideInVertically(initialOffsetY = { -it }) + androidx.compose.animation.fadeIn(),
            exit = androidx.compose.animation.slideOutVertically(targetOffsetY = { -it }) + androidx.compose.animation.fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 16.dp, start = 24.dp, end = 24.dp)
                    .border(1.dp, Color(0xFFFFD600).copy(alpha = 0.8f), RoundedCornerShape(16.dp))
                    .background(Color.Black, RoundedCornerShape(16.dp))
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(text = toastMessage, color = Color.White, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun AiChatRow(
    ai: AiAssistant,
    lastMessage: SmsMessage?,
    onClick: () -> Unit
) {
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val dateFormat = remember { SimpleDateFormat("dd.MM", Locale.getDefault()) }
    val colorIndex = AiAssistant.entries.indexOf(ai) % AI_COLORS.size
    val avatarColor = AI_COLORS[colorIndex]
    val icon = AI_ICONS[ai] ?: "🤖"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Аватар
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(avatarColor),
            contentAlignment = Alignment.Center
        ) {
            Text(text = icon, fontSize = 22.sp)
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = ai.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                if (lastMessage != null) {
                    val now = System.currentTimeMillis()
                    val isToday = now - lastMessage.date < 24 * 60 * 60 * 1000L
                    Text(
                        text = if (isToday) timeFormat.format(Date(lastMessage.date))
                               else dateFormat.format(Date(lastMessage.date)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            val preview = if (lastMessage != null) {
                val prefix = if (!lastMessage.isIncoming) "Вы: " else ""
                val body = lastMessage.body.replace(Regex("^\\[\\s*[^\\]]+\\s*\\]\\s*\n?"), "").trim()
                "$prefix$body"
            } else "Нет сообщений"
            Text(
                text = preview,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ChatDetailScreen(
    viewModel: HistoryViewModel,
    smsPhone: String,
    ai: AiAssistant,
    isOsaTheme: Boolean,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val messages by viewModel.messages.collectAsState()
    val selectedAi by viewModel.selectedAi.collectAsState()
    val listState = rememberLazyListState()
    var inputText by remember { mutableStateOf("") }

    val hasSmsPermission = ContextCompat.checkSelfPermission(
        context, Manifest.permission.SEND_SMS
    ) == PackageManager.PERMISSION_GRANTED

    BackHandler { onBack() }
    val currentAi = remember(ai) { ai.also { viewModel.selectAi(it) } }

    // Не показываем сообщения пока selectedAi не совпал с нужным
    val readyToShow = selectedAi == ai

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    Column(modifier = Modifier.fillMaxSize().imePadding()) {
        // Топбар
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
            }
            val colorIndex = AiAssistant.entries.indexOf(ai) % AI_COLORS.size
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(AI_COLORS[colorIndex]),
                contentAlignment = Alignment.Center
            ) {
                Text(text = AI_ICONS[ai] ?: "🤖", fontSize = 16.sp)
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(ai.displayName, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyLarge)
                Text("SMS · $smsPhone", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        HorizontalDivider()

        Box(modifier = Modifier.weight(1f)) {
            if (!readyToShow) {
                // Ждём пока selectedAi совпадёт — показываем пустой экран без мигания
                Box(modifier = Modifier.fillMaxSize())
            } else if (messages.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(AI_ICONS[ai] ?: "💬", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Нет сообщений с ${ai.displayName}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(messages, key = { "${it.id}_${it.isIncoming}" }) { msg ->
                        MessageBubble(msg, isOsaTheme)
                    }
                }
            }
        }

        HorizontalDivider()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Написать ${ai.displayName}...") },
                maxLines = 4,
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                )
            )
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .border(
                        1.5.dp,
                        if (inputText.isNotBlank() && hasSmsPermission) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = {
                        val text = inputText.trim()
                        if (text.isBlank()) return@IconButton
                        val smsText = "[ ${ai.displayName} ]\n$text"
                        SmsLauncher.sendDirect(context, smsPhone, smsText)
                        viewModel.addOutgoingMessage(smsText)
                        inputText = ""
                    },
                    enabled = inputText.isNotBlank() && hasSmsPermission,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Отправить",
                        tint = if (inputText.isNotBlank() && hasSmsPermission) MaterialTheme.colorScheme.primary
                               else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(msg: SmsMessage, isOsaTheme: Boolean = false) {
    val dateFormat = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()) }
    val dateStr = dateFormat.format(Date(msg.date))
    val alignment = if (msg.isIncoming) Alignment.Start else Alignment.End
    val bubbleColor = when {
        isOsaTheme && !msg.isIncoming -> MaterialTheme.colorScheme.surface
        isOsaTheme && msg.isIncoming -> MaterialTheme.colorScheme.surfaceVariant
        !msg.isIncoming -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val textColor = when {
        isOsaTheme && !msg.isIncoming -> Color.White
        isOsaTheme && msg.isIncoming -> MaterialTheme.colorScheme.onSurfaceVariant
        !msg.isIncoming -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val label = if (msg.isIncoming) "AI" else "Вы"

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = alignment) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        )
        Box(
            modifier = Modifier
                .then(
                    if (msg.isIncoming) Modifier.fillMaxWidth(0.85f)
                    else Modifier.fillMaxWidth(0.75f).wrapContentWidth(align = Alignment.End, unbounded = true)
                )
                .background(bubbleColor, RoundedCornerShape(12.dp))
                .then(
                    if (isOsaTheme) Modifier.border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                    else Modifier
                )
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            val displayText = msg.body.replace(Regex("^\\[\\s*[^\\]]+\\s*\\]\\s*\n?"), "").trim()
            Column {
                Text(text = displayText, color = textColor, style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.labelSmall,
                    color = textColor.copy(alpha = 0.6f),
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}
