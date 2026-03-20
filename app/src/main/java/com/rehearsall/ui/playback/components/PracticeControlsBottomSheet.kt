package com.rehearsall.ui.playback.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rehearsall.domain.model.PracticeMode
import com.rehearsall.domain.model.PracticeSettings
import com.rehearsall.playback.PracticeState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PracticeControlsBottomSheet(
    practiceState: PracticeState,
    settings: PracticeSettings,
    onModeChange: (PracticeMode) -> Unit,
    onRepeatCountChange: (Int) -> Unit,
    onGapBetweenRepsChange: (Long) -> Unit,
    onGapBetweenChunksChange: (Long) -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isPracticing = practiceState is PracticeState.Playing || practiceState is PracticeState.Pausing

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
        ) {
            Text(
                text = "Practice Controls",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 16.dp),
            )

            if (isPracticing) {
                // Active practice UI
                PracticeActiveContent(
                    state = practiceState,
                    onStop = onStop,
                    onSkipNext = onSkipNext,
                    onSkipPrevious = onSkipPrevious,
                )
            } else {
                // Settings UI
                PracticeSettingsContent(
                    settings = settings,
                    isComplete = practiceState is PracticeState.Complete,
                    onModeChange = onModeChange,
                    onRepeatCountChange = onRepeatCountChange,
                    onGapBetweenRepsChange = onGapBetweenRepsChange,
                    onGapBetweenChunksChange = onGapBetweenChunksChange,
                    onStart = onStart,
                )
            }
        }
    }
}

@Composable
private fun PracticeSettingsContent(
    settings: PracticeSettings,
    isComplete: Boolean,
    onModeChange: (PracticeMode) -> Unit,
    onRepeatCountChange: (Int) -> Unit,
    onGapBetweenRepsChange: (Long) -> Unit,
    onGapBetweenChunksChange: (Long) -> Unit,
    onStart: () -> Unit,
) {
    // Mode selector
    Text(
        text = "Mode",
        style = MaterialTheme.typography.labelLarge,
    )
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
    ) {
        PracticeMode.entries.forEach { mode ->
            FilterChip(
                selected = settings.mode == mode,
                onClick = { onModeChange(mode) },
                label = { Text(mode.displayName) },
            )
        }
    }

    Text(
        text = settings.mode.description,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    Spacer(modifier = Modifier.height(16.dp))

    // Repeat count
    if (settings.mode != PracticeMode.SEQUENTIAL_PLAY) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Repeats per chunk", style = MaterialTheme.typography.bodyMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { onRepeatCountChange((settings.repeatCount - 1).coerceAtLeast(1)) },
                    enabled = settings.repeatCount > 1,
                ) { Text("−", style = MaterialTheme.typography.titleLarge) }
                Text(
                    text = "${settings.repeatCount}",
                    style = MaterialTheme.typography.titleMedium,
                )
                IconButton(
                    onClick = { onRepeatCountChange((settings.repeatCount + 1).coerceAtMost(20)) },
                    enabled = settings.repeatCount < 20,
                ) { Text("+", style = MaterialTheme.typography.titleLarge) }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Gap between reps
        Text("Gap between reps: %.1fs".format(settings.gapBetweenRepsMs / 1000f), style = MaterialTheme.typography.bodyMedium)
        Slider(
            value = settings.gapBetweenRepsMs.toFloat(),
            onValueChange = { onGapBetweenRepsChange(it.toLong()) },
            valueRange = 0f..5000f,
            steps = 9,
            modifier = Modifier.fillMaxWidth(),
        )
    }

    // Gap between chunks
    Text("Gap between chunks: %.1fs".format(settings.gapBetweenChunksMs / 1000f), style = MaterialTheme.typography.bodyMedium)
    Slider(
        value = settings.gapBetweenChunksMs.toFloat(),
        onValueChange = { onGapBetweenChunksChange(it.toLong()) },
        valueRange = 0f..10000f,
        steps = 19,
        modifier = Modifier.fillMaxWidth(),
    )

    Spacer(modifier = Modifier.height(16.dp))

    if (isComplete) {
        Text(
            text = "Practice complete!",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp),
        )
    }

    Button(
        onClick = onStart,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
        Text(if (isComplete) "Restart Practice" else "Start Practice")
    }
}

@Composable
private fun PracticeActiveContent(
    state: PracticeState,
    onStop: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
) {
    when (state) {
        is PracticeState.Playing -> {
            Text(
                text = state.currentStep.label,
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = "Rep ${state.currentRep} / ${state.totalReps}",
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = "Step ${state.stepIndex + 1} of ${state.totalSteps}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { (state.stepIndex + 1).toFloat() / state.totalSteps.toFloat() },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        is PracticeState.Pausing -> {
            Text(
                text = "Pausing…",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            state.nextStep?.let {
                Text(
                    text = "Next: ${it.label}",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { (state.stepIndex + 1).toFloat() / state.totalSteps.toFloat() },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        else -> {}
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Transport
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onSkipPrevious) {
            Icon(Icons.Default.SkipPrevious, contentDescription = "Previous step")
        }

        OutlinedButton(onClick = onStop, modifier = Modifier.padding(horizontal = 16.dp)) {
            Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
            Text("Stop")
        }

        IconButton(onClick = onSkipNext) {
            Icon(Icons.Default.SkipNext, contentDescription = "Next step")
        }
    }
}
