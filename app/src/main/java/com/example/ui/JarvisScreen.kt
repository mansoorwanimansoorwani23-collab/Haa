package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.AudioWaveformVisualizer
import com.example.ui.components.QuantumOrb
import com.example.ui.components.SettingsDialog
import com.example.ui.components.TelemetryHeader
import com.example.ui.theme.QuantumCyanAccent
import com.example.ui.theme.QuantumDarkBg
import com.example.ui.theme.QuantumGreenBright
import com.example.ui.theme.QuantumGreenDim
import com.example.ui.theme.QuantumGreenGlow
import com.example.ui.theme.QuantumGreenPrimary
import com.example.ui.theme.QuantumRedAlert
import com.example.ui.theme.QuantumSurfaceBorder
import com.example.ui.theme.QuantumSurfaceCard
import com.example.ui.theme.QuantumSurfaceDark
import com.example.ui.theme.QuantumTextMuted
import com.example.ui.theme.QuantumTextPrimary
import com.example.ui.theme.QuantumTextSecondary

@Composable
fun JarvisScreen(
    viewModel: JarvisViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val state by viewModel.assistantState.collectAsStateWithLifecycle()
    val transcript by viewModel.currentTranscript.collectAsStateWithLifecycle()
    val responseText by viewModel.assistantSpeech.collectAsStateWithLifecycle()
    val actionBadge by viewModel.lastActionBadge.collectAsStateWithLifecycle()
    val amplitude by viewModel.audioAmplitude.collectAsStateWithLifecycle()
    val isContinuous by viewModel.isContinuousMode.collectAsStateWithLifecycle()
    val isWakeWord by viewModel.isWakeWordEnabled.collectAsStateWithLifecycle()
    val isBgActive by viewModel.isBackgroundServiceActive.collectAsStateWithLifecycle()
    val speechRate by viewModel.speechRate.collectAsStateWithLifecycle()
    val speechPitch by viewModel.speechPitch.collectAsStateWithLifecycle()
    val voices by viewModel.availableVoices.collectAsStateWithLifecycle()
    val customApiKey by viewModel.customApiKey.collectAsStateWithLifecycle()
    val memories by viewModel.memoryList.collectAsStateWithLifecycle()
    val errorMsg by viewModel.errorMessage.collectAsStateWithLifecycle()

    var showSettings by remember { mutableStateOf(false) }
    var showKeyboardInput by remember { mutableStateOf(false) }
    var typedInput by remember { mutableStateOf("") }

    // Request necessary runtime permissions
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }

    LaunchedEffect(Unit) {
        val permissionsToRequest = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.RECORD_AUDIO)
        }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.READ_CONTACTS)
        }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.CALL_PHONE)
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(QuantumDarkBg),
        containerColor = QuantumDarkBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(
                    top = WindowInsets.statusBars
                        .asPaddingValues()
                        .calculateTopPadding()
                )
                .imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Top HUD Telemetry
            TelemetryHeader(state = state)

            Spacer(modifier = Modifier.weight(0.1f))

            // 2. Central Quantum Orb
            QuantumOrb(
                state = state,
                amplitude = amplitude,
                onClick = { viewModel.toggleListening() },
                modifier = Modifier.testTag("quantum_orb_tap")
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 3. Audio Waveform Bars
            AudioWaveformVisualizer(
                amplitude = amplitude,
                state = state
            )

            Spacer(modifier = Modifier.weight(0.1f))

            // 4. Dialogue / Telemetry Display Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(QuantumSurfaceCard)
                    .border(
                        1.2.dp,
                        when (state) {
                            AssistantState.ERROR -> QuantumRedAlert
                            AssistantState.THINKING -> QuantumCyanAccent
                            AssistantState.LISTENING -> QuantumGreenBright
                            else -> QuantumSurfaceBorder
                        },
                        RoundedCornerShape(12.dp)
                    )
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Action badge if a tool executed
                    if (!actionBadge.isNullOrBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(QuantumGreenPrimary.copy(alpha = 0.15f))
                                    .border(1.dp, QuantumGreenPrimary.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "ACTION // $actionBadge",
                                    color = QuantumGreenBright,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }

                    // User speech transcript
                    if (transcript.isNotBlank()) {
                        Text(
                            text = "> \"$transcript\"",
                            color = QuantumCyanAccent,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }

                    // JARVIS Spoken response
                    Text(
                        text = responseText,
                        color = QuantumTextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Normal,
                        lineHeight = 21.sp
                    )

                    // Error notice if any
                    if (!errorMsg.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "! $errorMsg",
                            color = QuantumRedAlert,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 5. Quick Command Suggestion Chips
            val suggestions = listOf(
                "What time is it?",
                "Open WhatsApp",
                "System telemetry",
                "Open YouTube",
                "Search web",
                "Who made you?"
            )
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(suggestions) { prompt ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(QuantumSurfaceDark)
                            .border(1.dp, QuantumSurfaceBorder, RoundedCornerShape(16.dp))
                            .clickable { viewModel.sendTextCommand(prompt) }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = prompt,
                            color = QuantumTextSecondary,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // Keyboard text input drawer (if toggled)
            AnimatedVisibility(
                visible = showKeyboardInput,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = typedInput,
                        onValueChange = { typedInput = it },
                        placeholder = { Text("Transmit command to JARVIS...", color = QuantumTextMuted, fontSize = 12.sp) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("text_command_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = QuantumGreenPrimary,
                            unfocusedBorderColor = QuantumSurfaceBorder,
                            focusedTextColor = QuantumGreenBright,
                            unfocusedTextColor = QuantumTextPrimary
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = {
                            if (typedInput.isNotBlank()) {
                                viewModel.sendTextCommand(typedInput)
                                typedInput = ""
                                showKeyboardInput = false
                            }
                        })
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            if (typedInput.isNotBlank()) {
                                viewModel.sendTextCommand(typedInput)
                                typedInput = ""
                                showKeyboardInput = false
                            }
                        },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(QuantumGreenPrimary)
                            .testTag("send_command_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Send",
                            tint = QuantumDarkBg
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 6. Bottom Master Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Keyboard toggle
                IconButton(
                    onClick = { showKeyboardInput = !showKeyboardInput },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(QuantumSurfaceCard)
                        .border(1.dp, QuantumSurfaceBorder, CircleShape)
                        .testTag("keyboard_toggle_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Chat,
                        contentDescription = "Type text command",
                        tint = if (showKeyboardInput) QuantumGreenBright else QuantumTextSecondary
                    )
                }

                // Center: Big Glowing Quantum Voice Button
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    when (state) {
                                        AssistantState.SPEAKING -> QuantumGreenBright
                                        AssistantState.LISTENING -> QuantumCyanAccent
                                        AssistantState.THINKING -> QuantumGreenDim
                                        AssistantState.ERROR -> QuantumRedAlert
                                        AssistantState.IDLE -> QuantumGreenPrimary
                                    },
                                    when (state) {
                                        AssistantState.SPEAKING -> QuantumGreenDim
                                        AssistantState.LISTENING -> QuantumGreenDim
                                        else -> Color(0xFF004D1A)
                                    }
                                )
                            )
                        )
                        .border(2.dp, QuantumGreenBright, CircleShape)
                        .clickable { viewModel.toggleListening() }
                        .testTag("mic_toggle_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (state) {
                            AssistantState.SPEAKING -> Icons.Default.Stop // Tap to interrupt!
                            AssistantState.LISTENING -> Icons.Default.Mic
                            AssistantState.THINKING -> Icons.Default.MicOff
                            AssistantState.ERROR -> Icons.Default.Mic
                            AssistantState.IDLE -> Icons.Default.Mic
                        },
                        contentDescription = if (state == AssistantState.SPEAKING) "Stop Speaking" else "Toggle Listening",
                        tint = Color.Black,
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Right: Settings Dialog Toggle
                IconButton(
                    onClick = { showSettings = true },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(QuantumSurfaceCard)
                        .border(1.dp, QuantumSurfaceBorder, CircleShape)
                        .testTag("settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = QuantumTextSecondary
                    )
                }
            }

            // Bottom Subtext
            Text(
                text = "CONTINUOUS: ${if (isContinuous) "ONLINE" else "STANDBY"} // WAKE-WORD: ${if (isWakeWord) "ACTIVE" else "OFF"}",
                color = QuantumTextMuted,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
    }

    if (showSettings) {
        SettingsDialog(
            isWakeWordEnabled = isWakeWord,
            onToggleWakeWord = { viewModel.toggleWakeWord() },
            isContinuousMode = isContinuous,
            onToggleContinuousMode = { viewModel.toggleContinuousMode() },
            isBackgroundActive = isBgActive,
            onToggleBackgroundActive = { viewModel.toggleBackgroundService() },
            speechRate = speechRate,
            onSpeechRateChange = { viewModel.setSpeechRate(it) },
            speechPitch = speechPitch,
            onSpeechPitchChange = { viewModel.setSpeechPitch(it) },
            availableVoices = voices,
            onSelectVoice = { viewModel.setVoice(it) },
            customApiKey = customApiKey,
            onApiKeyChange = { viewModel.setCustomApiKey(it) },
            memories = memories,
            onDeleteMemory = { viewModel.deleteMemory(it) },
            onClearAllMemories = { viewModel.clearAllMemories() },
            onDismiss = { showSettings = false }
        )
    }
}
