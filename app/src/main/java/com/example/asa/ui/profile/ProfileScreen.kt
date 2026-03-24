package com.example.asa.ui.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.asa.viewmodel.ProfileViewModel
import com.yalantis.ucrop.UCrop
import java.io.File

@Composable
fun ProfileScreen(viewModel: ProfileViewModel, onOpenSettings: () -> Unit) {
    val user by viewModel.user.collectAsState()
    val avatarUri by viewModel.avatarUri.collectAsState()
    val context = LocalContext.current

    var showAvatarDialog by remember { mutableStateOf(false) }

    val cropLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val croppedUri = UCrop.getOutput(result.data ?: return@rememberLauncherForActivityResult)
        croppedUri?.let { viewModel.updateAvatar(it) }
    }

    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        val destFile = File(context.cacheDir, "avatar_crop_${System.currentTimeMillis()}.jpg")
        val destUri = Uri.fromFile(destFile)
        val options = UCrop.Options().apply {
            setCircleDimmedLayer(true)
            setShowCropGrid(false)
            setShowCropFrame(false)
        }
        val cropIntent = UCrop.of(uri, destUri)
            .withAspectRatio(1f, 1f)
            .withMaxResultSize(512, 512)
            .withOptions(options)
            .getIntent(context)
        cropLauncher.launch(cropIntent)
    }

    if (showAvatarDialog) {
        AlertDialog(
            onDismissRequest = { showAvatarDialog = false },
            title = { Text("Аватарка") },
            text = { Text("Что хотите сделать?") },
            confirmButton = {
                TextButton(onClick = {
                    showAvatarDialog = false
                    pickerLauncher.launch("image/*")
                }) { Text("Изменить") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAvatarDialog = false
                    viewModel.removeAvatar()
                }) {
                    Text("Удалить", color = MaterialTheme.colorScheme.error)
                }
            }
        )
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onOpenSettings) {
                Icon(imageVector = Icons.Default.Settings, contentDescription = "Настройки")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Внешняя прозрачная обводка — эффект тени/свечения
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .border(
                        width = 4.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .padding(3.dp)
                    .border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(18.dp)
                    )
                    .padding(2.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(vertical = 28.dp, horizontal = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (avatarUri != null) {
                        AsyncImage(
                            model = avatarUri,
                            contentDescription = "Аватарка",
                            modifier = Modifier
                                .size(96.dp)
                                .clip(CircleShape)
                                .clickable { showAvatarDialog = true }
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(96.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .clickable { pickerLauncher.launch("image/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Аватарка",
                                modifier = Modifier.size(56.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    Text(
                        text = user?.nickname ?: "—",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Black
                    )

                    Text(
                        text = user?.createdAt?.let { formatDate(it) } ?: "—",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Black.copy(alpha = 0.6f)
                    )

                    Text(
                        text = "Запросов отправлено: ${user?.requestsCount ?: 0}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Black
                    )
                }
            } // Box
        }
    }
}

private fun formatDate(raw: String): String {
    return try {
        val datePart = raw.substringBefore("T")
        val parts = datePart.split("-")
        if (parts.size == 3) "${parts[2]}.${parts[1]}.${parts[0]}" else raw
    } catch (e: Exception) {
        raw
    }
}
