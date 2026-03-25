package com.rehearsall.ui.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rehearsall.playback.PlaybackState

@Composable
fun MiniPlayer(
    isVisible: Boolean,
    trackName: String,
    playbackState: PlaybackState,
    onPlayPause: () -> Unit,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically { it },
        exit = slideOutVertically { it },
        modifier = modifier,
    ) {
        Surface(
            tonalElevation = 3.dp,
            shadowElevation = 4.dp,
        ) {
            Column {
                // Thin progress bar
                val progress =
                    if (playbackState.durationMs > 0) {
                        playbackState.positionMs.toFloat() / playbackState.durationMs.toFloat()
                    } else {
                        0f
                    }
                LinearProgressIndicator(
                    progress = { progress },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(2.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )

                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onTap)
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = trackName,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )

                    Text(
                        text = formatDuration(playbackState.positionMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp),
                    )

                    IconButton(
                        onClick = onPlayPause,
                        modifier = Modifier.size(40.dp),
                    ) {
                        Icon(
                            imageVector =
                                if (playbackState.isPlaying) {
                                    Icons.Default.Pause
                                } else {
                                    Icons.Default.PlayArrow
                                },
                            contentDescription = if (playbackState.isPlaying) "Pause" else "Play",
                        )
                    }
                }
            }
        }
    }
}
