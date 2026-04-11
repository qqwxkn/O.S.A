package com.example.asa.ui.profile

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.asa.viewmodel.ProfileViewModel
import java.io.File

@Composable
fun ProfileScreen(viewModel: ProfileViewModel, onOpenSettings: () -> Unit, onShowCropper: () -> Unit = {}, onHideCropper: () -> Unit = {}) {
    val user by viewModel.user.collectAsState()
    val avatarUri by viewModel.avatarUri.collectAsState()
    val avatarBitmap by viewModel.avatarBitmap.collectAsState()
    val context = LocalContext.current

    // Перезагружаем данные при каждом входе на вкладку
    LaunchedEffect(Unit) {
        viewModel.loadUser()
    }

    var showAvatarDialog by remember { mutableStateOf(false) }
    var cropUri by remember { mutableStateOf<Uri?>(null) }

    // Если открыт кроппер — показываем его поверх всего
    cropUri?.let { uri ->
        AvatarCropScreen(
            imageUri = uri,
            onCropped = { bitmap ->
                cropUri = null
                onHideCropper()
                viewModel.updateAvatarBitmap(bitmap)
            },
            onCancel = {
                cropUri = null
                onHideCropper()
            }
        )
        return
    }

    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            cropUri = it
            onShowCropper()
        }
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
        floatingActionButton = {}
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
                    .padding(top = 90.dp, start = 16.dp, end = 16.dp, bottom = 16.dp)
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
                        color = MaterialTheme.colorScheme.surface,
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
                    if (avatarBitmap != null) {
                        Image(
                            bitmap = avatarBitmap!!.asImageBitmap(),
                            contentDescription = "Аватарка",
                            modifier = Modifier
                                .size(96.dp)
                                .clip(CircleShape)
                                .clickable { showAvatarDialog = true }
                        )
                    } else if (avatarUri != null) {
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
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { pickerLauncher.launch("image/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Аватарка",
                                modifier = Modifier.size(56.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Text(
                        text = user?.nickname ?: "—",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "Дата регистрации: ${user?.createdAt?.let { formatDate(it) } ?: "—"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
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
