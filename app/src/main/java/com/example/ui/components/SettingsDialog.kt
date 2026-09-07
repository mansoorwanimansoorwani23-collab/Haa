package com.example.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.speech.tts.Voice
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.data.MemoryEntity
import com.example.ui.theme.QuantumCyanAccent
import com.example.ui.theme.QuantumDarkBg
import com.example.ui.theme.QuantumGreenBright
import com.example.ui.theme.QuantumGreenDim
import com.example.ui.theme.QuantumGreenPrimary
import com.example.ui.theme.QuantumRedAlert
import com.example.ui.theme.QuantumSurfaceBorder
import com.example.ui.theme.QuantumSurfaceCard
import com.example.ui.theme.QuantumTextMuted
import com.example.ui.theme.QuantumTextPrimary
import com.example.ui.theme.QuantumTextSecondary

@Composable
fun SettingsDialog(
    isWakeWordEnabled: Boolean,
    onToggleWakeWord: () -> Unit,
    isContinuousMode: Boolean,
    onToggleContinuousMode: () -> Unit,
    isBackgroundActive: Boolean,
    onToggleBackgroundActive: () -> Unit,
    speechRate: Float,
    onSpeechRateChange: (Float) -> Unit,
    speechPitch: Float,
    onSpeechPitchChange: (Float) -> Unit,
    availableVoices: List<Voice>,
    onSelectVoice: (Voice) -> Unit,
    customApiKey: String,
    onApiKeyChange: (String) -> Unit,
    memories: List<MemoryEntity>,
    onDeleteMemory: (String) -> Unit,
    onClearAllMemories: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var currentTab by remember { mutableStateOf(0) } // 0: Voice & Core, 1: Memory, 2: System
    var showClearConfirm by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f)
                .clip(RoundedCornerShape(16.dp))
                .background(QuantumDarkBg)
                .border(1.5.dp, QuantumSurfaceBorder, RoundedCornerShape(16.dp))
                .padding(20.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "JARVIS TERMINAL SETTINGS",
                            color = QuantumGreenBright,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.2.sp
                        )
                        Text(
                            text = "ARCHITECT: RAUF // KERNEL CONFIG",
                            color = QuantumCyanAccent,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = QuantumTextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(QuantumSurfaceCard)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    val tabs = listOf("VOICE & AI", "MEMORY", "SECURITY")
                    tabs.forEachIndexed { index, title ->
                        val isSelected = currentTab == index
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) QuantumGreenPrimary.copy(alpha = 0.2f) else Color.Transparent)
                                .border(
                                    1.dp,
                                    if (isSelected) QuantumGreenPrimary else Color.Transparent,
                                    RoundedCornerShape(6.dp)
                                )
                                .clickable { currentTab = index }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = title,
                                color = if (isSelected) QuantumGreenBright else QuantumTextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Tab Content
                when (currentTab) {
                    0 -> VoiceAndAiTab(
                        isWakeWordEnabled = isWakeWordEnabled,
                        onToggleWakeWord = onToggleWakeWord,
                        isContinuousMode = isContinuousMode,
                        onToggleContinuousMode = onToggleContinuousMode,
                        isBackgroundActive = isBackgroundActive,
                        onToggleBackgroundActive = onToggleBackgroundActive,
                        speechRate = speechRate,
                        onSpeechRateChange = onSpeechRateChange,
                        speechPitch = speechPitch,
                        onSpeechPitchChange = onSpeechPitchChange,
                        availableVoices = availableVoices,
                        onSelectVoice = onSelectVoice,
                        customApiKey = customApiKey,
                        onApiKeyChange = onApiKeyChange
                    )
                    1 -> MemoryTab(
                        memories = memories,
                        onDeleteMemory = onDeleteMemory,
                        onClearAll = { showClearConfirm = true }
                    )
                    2 -> SystemSecurityTab(context = context)
                }
            }
        }
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = {
                Text(
                    text = "PURGE QUANTUM MEMORY?",
                    color = QuantumRedAlert,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "This will permanently erase all saved conversation logs, learned user preferences, and short-term facts.",
                    color = QuantumTextPrimary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClearAllMemories()
                        showClearConfirm = false
                    }
                ) {
                    Text("PURGE ALL", color = QuantumRedAlert, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text("CANCEL", color = QuantumTextSecondary)
                }
            },
            containerColor = QuantumDarkBg,
            shape = RoundedCornerShape(12.dp)
        )
    }
}

@Composable
private fun VoiceAndAiTab(
    isWakeWordEnabled: Boolean,
    onToggleWakeWord: () -> Unit,
    isContinuousMode: Boolean,
    onToggleContinuousMode: () -> Unit,
    isBackgroundActive: Boolean,
    onToggleBackgroundActive: () -> Unit,
    speechRate: Float,
    onSpeechRateChange: (Float) -> Unit,
    speechPitch: Float,
    onSpeechPitchChange: (Float) -> Unit,
    availableVoices: List<Voice>,
    onSelectVoice: (Voice) -> Unit,
    customApiKey: String,
    onApiKeyChange: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Wake Word Toggle
            SettingsToggleRow(
                title = "Wake Word Detection",
                subtitle = "Summon JARVIS by uttering \"Hey JARVIS\" or \"JARVIS\"",
                icon = Icons.Default.Mic,
                checked = isWakeWordEnabled,
                onCheckedChange = { onToggleWakeWord() }
            )
        }

        item {
            // Continuous Conversation Mode
            SettingsToggleRow(
                title = "Continuous Conversation",
                subtitle = "Re-opens microphone automatically after JARVIS speaks",
                icon = Icons.Default.GraphicEq,
                checked = isContinuousMode,
                onCheckedChange = { onToggleContinuousMode() }
            )
        }

        item {
            // Background Operation
            SettingsToggleRow(
                title = "Background Voice Core",
                subtitle = "Enables Android Foreground Service with status notification",
                icon = Icons.Default.Security,
                checked = isBackgroundActive,
                onCheckedChange = { onToggleBackgroundActive() }
            )
        }

        item {
            // Speech Rate Slider
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(QuantumSurfaceCard)
                    .border(1.dp, QuantumSurfaceBorder, RoundedCornerShape(8.dp))
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Android TTS Speech Rate",
                        color = QuantumTextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "${String.format("%.1f", speechRate)}x",
                        color = QuantumGreenBright,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Slider(
                    value = speechRate,
                    onValueChange = onSpeechRateChange,
                    valueRange = 0.6f..1.6f,
                    steps = 9,
                    colors = SliderDefaults.colors(
                        thumbColor = QuantumGreenBright,
                        activeTrackColor = QuantumGreenPrimary,
                        inactiveTrackColor = QuantumSurfaceBorder
                    )
                )
            }
        }

        item {
            // Speech Pitch Slider
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(QuantumSurfaceCard)
                    .border(1.dp, QuantumSurfaceBorder, RoundedCornerShape(8.dp))
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Vocal Pitch (Masculine Cadence)",
                        color = QuantumTextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "${String.format("%.2f", speechPitch)}",
                        color = QuantumCyanAccent,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Slider(
                    value = speechPitch,
                    onValueChange = onSpeechPitchChange,
                    valueRange = 0.65f..1.35f,
                    steps = 13,
                    colors = SliderDefaults.colors(
                        thumbColor = QuantumCyanAccent,
                        activeTrackColor = QuantumCyanAccent,
                        inactiveTrackColor = QuantumSurfaceBorder
                    )
                )
            }
        }

        item {
            // Gemini API Key input
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(QuantumSurfaceCard)
                    .border(1.dp, QuantumSurfaceBorder, RoundedCornerShape(8.dp))
                    .padding(14.dp)
            ) {
                Text(
                    text = "Gemini API Configuration",
                    color = QuantumTextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "Configured via AI Studio Secrets Panel or provide manual override:",
                    color = QuantumTextSecondary,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                OutlinedTextField(
                    value = customApiKey,
                    onValueChange = onApiKeyChange,
                    placeholder = { Text("AI Studio Managed Key (Active)", color = QuantumTextMuted, fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = QuantumGreenPrimary,
                        unfocusedBorderColor = QuantumSurfaceBorder,
                        focusedTextColor = QuantumGreenBright,
                        unfocusedTextColor = QuantumTextPrimary
                    ),
                    singleLine = true
                )
            }
        }

        if (availableVoices.isNotEmpty()) {
            item {
                Text(
                    text = "AVAILABLE DEVICE MALE/SYSTEM VOICES (${availableVoices.size})",
                    color = QuantumGreenBright,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            items(availableVoices.take(6)) { voice ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(QuantumSurfaceCard)
                        .border(1.dp, QuantumSurfaceBorder, RoundedCornerShape(6.dp))
                        .clickable { onSelectVoice(voice) }
                        .padding(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = voice.name,
                                color = QuantumTextPrimary,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "Locale: ${voice.locale.displayName}",
                                color = QuantumTextMuted,
                                fontSize = 10.sp
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = null,
                            tint = QuantumGreenDim
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MemoryTab(
    memories: List<MemoryEntity>,
    onDeleteMemory: (String) -> Unit,
    onClearAll: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "QUANTUM MEMORY BANKS (${memories.size})",
                color = QuantumGreenBright,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )

            if (memories.isNotEmpty()) {
                OutlinedButton(
                    onClick = onClearAll,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = QuantumRedAlert),
                    border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(QuantumRedAlert))
                ) {
                    Text("PURGE ALL", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (memories.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No user preferences stored in memory banks yet.\nInstruct JARVIS with \"Remember that...\"",
                    color = QuantumTextMuted,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(memories) { mem ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(QuantumSurfaceCard)
                            .border(1.dp, QuantumSurfaceBorder, RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = mem.key.uppercase(),
                                    color = QuantumCyanAccent,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = mem.value,
                                    color = QuantumTextPrimary,
                                    fontSize = 13.sp
                                )
                            }
                            IconButton(onClick = { onDeleteMemory(mem.key) }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = QuantumRedAlert
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SystemSecurityTab(context: Context) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "SYSTEM ARCHITECTURE & PERMISSIONS",
            color = QuantumGreenBright,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )

        val permissions = listOf(
            Pair("Audio Recording (STT)", Manifest.permission.RECORD_AUDIO),
            Pair("Address Book (Contacts)", Manifest.permission.READ_CONTACTS),
            Pair("Phone Dialer (Calls)", Manifest.permission.CALL_PHONE),
            Pair("System Notifications", Manifest.permission.POST_NOTIFICATIONS)
        )

        permissions.forEach { (label, perm) ->
            val granted = ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(QuantumSurfaceCard)
                    .border(1.dp, QuantumSurfaceBorder, RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = label,
                        color = QuantumTextPrimary,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = if (granted) "GRANTED" else "PENDING",
                        color = if (granted) QuantumGreenBright else QuantumRedAlert,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(QuantumSurfaceCard)
                .border(1.dp, QuantumSurfaceBorder, RoundedCornerShape(8.dp))
                .padding(14.dp)
        ) {
            Column {
                Text(
                    text = "PROJECT METADATA",
                    color = QuantumCyanAccent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "System Architect: Rauf\nModel: Gemini 3.5 Flash\nVoice Engine: Native Android TTS (No third-party paid API)\nInput Engine: Android SpeechRecognizer\nDatabase: SQLite Room Encrypted",
                    color = QuantumTextSecondary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(QuantumSurfaceCard)
            .border(1.dp, QuantumSurfaceBorder, RoundedCornerShape(8.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = QuantumGreenPrimary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = title,
                        color = QuantumTextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = subtitle,
                        color = QuantumTextMuted,
                        fontSize = 10.sp
                    )
                }
            }

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = QuantumGreenBright,
                    checkedTrackColor = QuantumGreenPrimary.copy(alpha = 0.5f),
                    uncheckedThumbColor = QuantumTextMuted,
                    uncheckedTrackColor = QuantumSurfaceBorder
                )
            )
        }
    }
}
