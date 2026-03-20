package com.rehearsall.ui.playback.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rehearsall.playback.RepeatMode

@Composable
fun TransportBar(
    isPlaying: Boolean,
    repeatMode: RepeatMode,
    shuffleEnabled: Boolean,
    onPlayPause: () -> Unit,
    onSkipForward: () -> Unit,
    onSkipBackward: () -> Unit,
    onSkipToNext: () -> Unit,
    onSkipToPrevious: () -> Unit,
    onCycleRepeat: () -> Unit,
    onToggleShuffle: () -> Unit,
    modifier: Modifier = Modifier,
    skipIncrementLabel: String = "5 seconds",
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Shuffle
        IconButton(
            onClick = onToggleShuffle,
            colors = if (shuffleEnabled) {
                IconButtonDefaults.iconButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary,
                )
            } else {
                IconButtonDefaults.iconButtonColors()
            },
        ) {
            Icon(
                imageVector = Icons.Default.Shuffle,
                contentDescription = if (shuffleEnabled) "Shuffle on" else "Shuffle off",
            )
        }

        // Skip previous
        IconButton(onClick = onSkipToPrevious) {
            Icon(
                imageVector = Icons.Default.SkipPrevious,
                contentDescription = "Previous track",
            )
        }

        // Skip backward (10s)
        IconButton(onClick = onSkipBackward) {
            Icon(
                imageVector = Icons.Default.FastRewind,
                contentDescription = "Skip back $skipIncrementLabel",
            )
        }

        // Play/Pause (large FAB)
        FloatingActionButton(
            onClick = onPlayPause,
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(64.dp),
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                modifier = Modifier.size(36.dp),
            )
        }

        // Skip forward (10s)
        IconButton(onClick = onSkipForward) {
            Icon(
                imageVector = Icons.Default.FastForward,
                contentDescription = "Skip forward $skipIncrementLabel",
            )
        }

        // Skip next
        IconButton(onClick = onSkipToNext) {
            Icon(
                imageVector = Icons.Default.SkipNext,
                contentDescription = "Next track",
            )
        }

        // Repeat mode
        IconButton(
            onClick = onCycleRepeat,
            colors = if (repeatMode != RepeatMode.OFF) {
                IconButtonDefaults.iconButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary,
                )
            } else {
                IconButtonDefaults.iconButtonColors()
            },
        ) {
            Icon(
                imageVector = when (repeatMode) {
                    RepeatMode.OFF -> Icons.Default.Repeat
                    RepeatMode.ONE -> Icons.Default.RepeatOne
                    RepeatMode.ALL -> Icons.Default.Repeat
                },
                contentDescription = when (repeatMode) {
                    RepeatMode.OFF -> "Repeat off"
                    RepeatMode.ONE -> "Repeat one"
                    RepeatMode.ALL -> "Repeat all"
                },
            )
        }
    }
}
