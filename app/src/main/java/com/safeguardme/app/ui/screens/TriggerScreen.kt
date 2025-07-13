// ui/screens/TriggerScreen.kt - Fixed without Auto-Navigation
package com.safeguardme.app.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.safeguardme.app.managers.KeywordMatchResult
import com.safeguardme.app.managers.MatchType
import com.safeguardme.app.managers.TranscriptionResult
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

    // Transcription states
    val isTranscribing by viewModel.isTranscribing.collectAsState()
    val transcriptionResult by viewModel.transcriptionResult.collectAsState()
    val keywordMatchResult by viewModel.keywordMatchResult.collectAsState()
    val showTranscriptionDialog by viewModel.showTranscriptionDialog.collectAsState()

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

    // Load existing trigger data on screen start
    LaunchedEffect(Unit) {
        viewModel.loadExistingTriggerData()
    }

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
                        text = "Choose a unique keyword and record a voice sample. We'll transcribe your recording to verify accuracy.",
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
                enabled = !isRecording && !isSaving && !isTranscribing
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

                    // Recording Status
                    when {
                        isRecording -> {
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
                        }

                        isTranscribing -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(32.dp))
                                Text(
                                    text = "Transcribing your voice...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        hasRecording && keywordMatchResult != null -> {
                            // Show transcription verification result
                            TranscriptionResultCard(
                                keyword = keyword,
                                matchResult = keywordMatchResult!!,
                                transcriptionResult = transcriptionResult
                            )
                        }

                        hasRecording -> {
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

                        else -> {
                            Text(
                                text = "Record yourself saying your keyword clearly",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                enabled = !isRecording && !isTranscribing
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
                                enabled = !isRecording && !isPlaying && !isTranscribing
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
                    text = when {
                        isSaving -> "Saving..."
                        keywordMatchResult?.isMatch == true -> "Save Verified Voice Trigger"
                        hasRecording -> "Save Voice Trigger (Unverified)"
                        else -> "Save Voice Trigger"
                    }
                )
            }

            Spacer(modifier = Modifier.weight(1f))
        }

        // Transcription Results Dialog
        if (showTranscriptionDialog) {
            TranscriptionDialog(
                keyword = keyword,
                transcriptionResult = transcriptionResult,
                keywordMatchResult = keywordMatchResult,
                onAccept = viewModel::acceptTranscription,
                onRetry = viewModel::retryTranscription,
                onSkip = viewModel::skipTranscriptionVerification,
                onDismiss = viewModel::dismissTranscriptionDialog
            )
        }

        // ✅ FIXED: Success Message - No Auto-Navigation
        successMessage?.let { message ->
            Snackbar(
                modifier = Modifier.padding(16.dp),
                action = {
                    TextButton(onClick = { viewModel.clearSuccessMessage() }) {
                        Text("Dismiss")
                    }
                }
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

/**
 * Card showing transcription verification results
 */
@Composable
private fun TranscriptionResultCard(
    keyword: String,
    matchResult: KeywordMatchResult,
    transcriptionResult: TranscriptionResult?
) {
    val isMatch = matchResult.isMatch
    val cardColor = if (isMatch) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.errorContainer
    }

    val iconColor = if (isMatch) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onErrorContainer
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = cardColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (isMatch) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )

                Text(
                    text = if (isMatch) "Keyword Verified!" else "Verification Warning",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = iconColor
                )
            }

            Text(
                text = "Expected: \"$keyword\"",
                style = MaterialTheme.typography.bodySmall,
                color = iconColor
            )

            Text(
                text = "Heard: \"${matchResult.transcribedText}\"",
                style = MaterialTheme.typography.bodySmall,
                color = iconColor
            )

            // Match type and confidence
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Match: ${matchResult.matchType.name.lowercase().replace('_', ' ')}",
                    style = MaterialTheme.typography.labelSmall,
                    color = iconColor
                )

                Text(
                    text = "Confidence: ${(matchResult.confidence * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = iconColor
                )
            }

            if (!isMatch) {
                Text(
                    text = "⚠️ The transcription doesn't match your keyword. You can still save, but consider re-recording for better accuracy.",
                    style = MaterialTheme.typography.bodySmall,
                    color = iconColor
                )
            }
        }
    }
}

/**
 * Detailed transcription results dialog
 */
@Composable
private fun TranscriptionDialog(
    keyword: String,
    transcriptionResult: TranscriptionResult?,
    keywordMatchResult: KeywordMatchResult?,
    onAccept: () -> Unit,
    onRetry: () -> Unit,
    onSkip: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.RecordVoiceOver,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text("Voice Transcription")
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "We transcribed your recording:",
                    style = MaterialTheme.typography.bodyMedium
                )

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = "Expected: \"$keyword\"",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Heard: \"${keywordMatchResult?.transcribedText ?: "Unknown"}\"",
                            style = MaterialTheme.typography.bodySmall
                        )

                        transcriptionResult?.let { result ->
                            if (result.alternativeTexts.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Alternatives: ${result.alternativeTexts.take(2).joinToString(", ")}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                val isMatch = keywordMatchResult?.isMatch ?: false
                val matchText = when (keywordMatchResult?.matchType) {
                    MatchType.EXACT -> "✅ Perfect match!"
                    MatchType.CONTAINS -> "✅ Keyword found in transcription"
                    MatchType.FUZZY -> "⚠️ Similar but not exact match"
                    MatchType.NO_MATCH -> "❌ No match found"
                    null -> "❌ Transcription failed"
                }

                Text(
                    text = matchText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isMatch) Color.Green else MaterialTheme.colorScheme.error
                )
            }
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(onClick = onRetry) {
                    Text("Retry")
                }

                if (keywordMatchResult?.isMatch != true) {
                    TextButton(onClick = onSkip) {
                        Text("Skip Verification")
                    }
                }

                Button(onClick = onAccept) {
                    Text("Accept")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
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