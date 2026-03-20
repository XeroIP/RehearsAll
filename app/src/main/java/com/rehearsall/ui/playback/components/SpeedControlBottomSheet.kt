package com.rehearsall.ui.playback.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpeedControlBottomSheet(
    currentSpeed: Float,
    onSpeedChange: (Float) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Playback Speed",
                style = MaterialTheme.typography.titleMedium,
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Current speed display
            Text(
                text = "%.2fx".format(currentSpeed),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Fine-tune buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(
                    onClick = {
                        val newSpeed = ((currentSpeed - 0.05f) * 20).toInt() / 20f
                        onSpeedChange(newSpeed.coerceAtLeast(0.25f))
                    },
                ) {
                    Text("- 0.05")
                }

                OutlinedButton(
                    onClick = {
                        val newSpeed = ((currentSpeed + 0.05f) * 20).toInt() / 20f
                        onSpeedChange(newSpeed.coerceAtMost(3.0f))
                    },
                ) {
                    Text("+ 0.05")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Slider
            Slider(
                value = currentSpeed,
                onValueChange = { speed ->
                    val rounded = (speed * 20).toInt() / 20f
                    onSpeedChange(rounded)
                },
                valueRange = 0.25f..3.0f,
                modifier = Modifier.fillMaxWidth(),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("0.25x", style = MaterialTheme.typography.labelSmall)
                Text("3.0x", style = MaterialTheme.typography.labelSmall)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Preset chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val presets = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
                presets.forEach { preset ->
                    FilterChip(
                        selected = currentSpeed == preset,
                        onClick = { onSpeedChange(preset) },
                        label = { Text("${preset}x") },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Reset button
            if (currentSpeed != 1.0f) {
                TextButton(onClick = { onSpeedChange(1.0f) }) {
                    Text("Reset to 1.0x")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
