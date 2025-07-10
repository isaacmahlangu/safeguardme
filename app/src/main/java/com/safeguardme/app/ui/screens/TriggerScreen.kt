// ui/screens/TriggerScreen.kt
package com.safeguardme.app.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.safeguardme.app.ui.viewmodels.TriggerViewModel
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TriggerScreen(
    navController: NavController,
    viewModel: TriggerViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    // Form states
    val keyword by viewModel.keyword.collectAsState()
    val keywordError by viewModel.keywordError.collectAsState()

    // Recording states
    val isRecording by viewModel.isRecording.collectAsState()
    val recordingTime by viewModel.recordingTime.collectAsState()
    val amplitude by viewModel.amplitude.collectAsState()
    val hasRecording by viewModel.hasRecording.collectAsState()

    // Playback states
    val isPlaying by viewModel.isPlaying.collectAsState()

    // Save states
    val isSaving by viewModel.isSaving.collectAsState()
    val canSave by viewModel.canSave.collectAsState()

    // Detection status
    val detectionEnabled by viewModel.detectionEnabled.collectAsState()

    // UI states
    val error by viewModel.error.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()

    // Animations
    val recordButtonScale by animateFloatAsState(
        targetValue = if (isRecording) 1.2f else 1f,
        animationSpec = tween(200), label = "record_scale"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Voice Trigger Setup",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {

            Spacer(modifier = Modifier.height(16.dp))

            // Detection Status Pill
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = if (detectionEnabled)
                    MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (detectionEnabled) Icons.Default.Mic else Icons.Default.MicOff,
                        contentDescription = null,
                        tint = if (detectionEnabled)
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Listener: ${if (detectionEnabled) "ON" else "OFF"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (detectionEnabled)
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Info Card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.RecordVoiceOver,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Voice Keyword Setup",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Choose a unique keyword and record a voice sample. This will allow hands-free emergency activation when you say your keyword.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // Keyword Input
            OutlinedTextField(
                value = keyword,
                onValueChange = viewModel::updateKeyword,
                label = { Text("Your Secret Keyword") },
                placeholder = { Text("e.g., Safeguard, Phoenix, Guardian") },
                isError = keywordError != null,
                supportingText = keywordError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                leadingIcon = {
                    Icon(Icons.Default.Key, contentDescription = null)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isRecording && !isSaving
            )

            // Recording Section
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Voice Sample",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    if (!isRecording && !hasRecording) {
                        Text(
                            text = "Record yourself saying your keyword clearly",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Waveform or Recording Status
                    if (isRecording) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            WaveformVisualizer(
                                amplitude = amplitude,
                                isActive = isRecording
                            )
                            Text(
                                text = "Recording... ${recordingTime}s / 5s",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else if (hasRecording) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color.Green
                            )
                            Text(
                                text = "Recording complete (${recordingTime}s)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Green
                            )
                        }
                    }

                    // Recording Controls
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Record Button
                        FloatingActionButton(
                            onClick = {
                                if (isRecording) {
                                    viewModel.stopRecording()
                                } else {
                                    viewModel.startRecording()
                                }
                            },
                            modifier = Modifier.scale(recordButtonScale),
                            containerColor = if (isRecording)
                                MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.primary
                        ) {
                            Icon(
                                imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                                contentDescription = if (isRecording) "Stop recording" else "Start recording",
                                tint = Color.White
                            )
                        }

                        // Playback Button
                        if (hasRecording) {
                            IconButton(
                                onClick = { viewModel.playRecording() },
                                enabled = !isRecording
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlaying) "Stop playback" else "Play recording",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        // Delete Button
                        if (hasRecording) {
                            IconButton(
                                onClick = { viewModel.deleteRecording() },
                                enabled = !isRecording && !isPlaying
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete recording",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }

            // Save Button
            Button(
                onClick = { viewModel.saveKeywordAndSample() },
                modifier = Modifier.fillMaxWidth(),
                enabled = canSave
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = if (isSaving) "Saving..." else "Save Voice Trigger"
                )
            }

            Spacer(modifier = Modifier.weight(1f))
        }

        // Success Message
        successMessage?.let { message ->
            LaunchedEffect(message) {
                kotlinx.coroutines.delay(3000)
                viewModel.clearSuccessMessage()
                navController.navigateUp()
            }

            Snackbar(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(message)
            }
        }

        // Error Message
        error?.let { errorMessage ->
            Snackbar(
                modifier = Modifier.padding(16.dp),
                action = {
                    TextButton(onClick = { viewModel.clearError() }) {
                        Text("Dismiss")
                    }
                }
            ) {
                Text(errorMessage)
            }
        }
    }
}

@Composable
private fun WaveformVisualizer(
    amplitude: Int,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")
    val animatedAmplitude by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (isActive) 1f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ), label = "amplitude"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp)
    ) {
        drawWaveform(
            amplitude = if (isActive) amplitude.toFloat() else 0f,
            animatedAmplitude = animatedAmplitude,
            color = androidx.compose.ui.graphics.Color.Blue
        )
    }
}

private fun DrawScope.drawWaveform(
    amplitude: Float,
    animatedAmplitude: Float,
    color: androidx.compose.ui.graphics.Color
) {
    val width = size.width
    val height = size.height
    val centerY = height / 2
    val barCount = 20
    val barWidth = width / barCount / 2

    for (i in 0 until barCount) {
        val x = (i * width / barCount) + barWidth / 2
        val normalizedAmplitude = (amplitude / 1000f).coerceIn(0f, 1f)
        val waveHeight = (sin(i * 0.5f + animatedAmplitude * 10) * normalizedAmplitude * height / 3).coerceAtLeast(4f)

        drawRect(
            color = color,
            topLeft = androidx.compose.ui.geometry.Offset(x - barWidth / 2, centerY - waveHeight / 2),
            size = androidx.compose.ui.geometry.Size(barWidth, waveHeight)
        )
    }
}