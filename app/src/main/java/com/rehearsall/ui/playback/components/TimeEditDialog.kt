package com.rehearsall.ui.playback.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import com.rehearsall.ui.common.formatDuration

/**
 * Dialog for editing a time position. Shows a text field pre-filled with the
 * current time in M:SS format. Accepts input as M:SS or just seconds.
 */
@Composable
fun TimeEditDialog(
    title: String,
    currentMs: Long,
    durationMs: Long,
    onConfirm: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf(formatDuration(currentMs)) }
    val parsed = parseTimeInput(text)
    val isValid = parsed != null && parsed in 0..durationMs

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Time (M:SS)") },
                    singleLine = true,
                    isError = text.isNotBlank() && !isValid,
                    supportingText = {
                        if (text.isNotBlank() && !isValid) {
                            Text("Enter time as M:SS (max ${formatDuration(durationMs)})")
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = "Duration: ${formatDuration(durationMs)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { parsed?.let { onConfirm(it) } },
                enabled = isValid,
            ) { Text("Set") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

/**
 * Parses time input in several formats:
 * - "1:30" → 90000 ms
 * - "90" → 90000 ms (interpreted as seconds)
 * - "1:05" → 65000 ms
 * Returns null if input can't be parsed.
 */
internal fun parseTimeInput(input: String): Long? {
    val trimmed = input.trim()
    if (trimmed.isEmpty()) return null

    return if (trimmed.contains(":")) {
        val parts = trimmed.split(":")
        if (parts.size != 2) return null
        val minutes = parts[0].toLongOrNull() ?: return null
        val seconds = parts[1].toLongOrNull() ?: return null
        if (minutes < 0 || seconds < 0 || seconds >= 60) return null
        (minutes * 60 + seconds) * 1000
    } else {
        val seconds = trimmed.toLongOrNull() ?: return null
        if (seconds < 0) return null
        seconds * 1000
    }
}
