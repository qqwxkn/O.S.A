package com.example.asa.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.asa.model.AiAssistant
import com.example.asa.model.AppTheme
import com.example.asa.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    viewModel: SettingsViewModel,
    onOpenSecurity: () -> Unit,
    onLogout: () -> Unit,
    onDismiss: () -> Unit
) {
    val theme by viewModel.theme.collectAsState()
    val defaultAi by viewModel.defaultAi.collectAsState()
    val smsPhone by viewModel.smsPhone.collectAsState()

    var themeExpanded by remember { mutableStateOf(false) }
    var aiExpanded by remember { mutableStateOf(false) }
    var smsExpanded by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(16.dp)) {

            // Секция "Тема"
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { themeExpanded = !themeExpanded }
                    .padding(vertical = 8.dp)
            ) {
                Text(
                    text = "Тема",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (themeExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null
                )
            }
            if (themeExpanded) {
                listOf(
                    AppTheme.DARK to "Тёмная",
                    AppTheme.LIGHT to "Светлая",
                    AppTheme.SYSTEM to "Системная"
                ).forEach { (value, label) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        RadioButton(selected = theme == value, onClick = { viewModel.setTheme(value) })
                        Text(text = label)
                    }
                }
            }

            Divider()

            // Секция "AI по умолчанию"
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { aiExpanded = !aiExpanded }
                    .padding(vertical = 8.dp)
            ) {
                Text(
                    text = "AI по умолчанию",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (aiExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null
                )
            }
            if (aiExpanded) {
                AiAssistant.entries.forEach { ai ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        RadioButton(selected = defaultAi == ai, onClick = { viewModel.setDefaultAi(ai) })
                        Text(text = ai.displayName)
                    }
                }
            }

            Divider()

            // Секция "Номер для SMS"
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { smsExpanded = !smsExpanded }
                    .padding(vertical = 8.dp)
            ) {
                Text(
                    text = "Номер для SMS",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (smsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null
                )
            }
            if (smsExpanded) {
                listOf(
                    "89155399434" to "8 915 539 94 34",
                    "89003578107" to "8 900 357 81 07"
                ).forEach { (value, label) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        RadioButton(selected = smsPhone == value, onClick = { viewModel.setSmsPhone(value) })
                        Text(text = label)
                    }
                }
            }

            Divider(modifier = Modifier.padding(vertical = 4.dp))

            TextButton(onClick = onOpenSecurity, modifier = Modifier.fillMaxWidth()) {
                Text(text = "Безопасность")
            }

            TextButton(
                onClick = { viewModel.logout(); onLogout() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Выйти", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
