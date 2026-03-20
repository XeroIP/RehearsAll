package com.rehearsall.ui.playback.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Interactive waveform view with pinch-to-zoom, horizontal scroll,
 * tap-to-seek, and a playback cursor that auto-scrolls during playback.
 *
 * @param amplitudes Normalized amplitude data (0.0–1.0)
 * @param positionFraction Current playback position as fraction (0.0–1.0)
 * @param isPlaying Whether playback is active (enables auto-scroll)
 * @param onSeek Called with position fraction (0.0–1.0) when user taps or drags
 * @param height Height of the waveform view
 */
@Composable
fun WaveformView(
    amplitudes: FloatArray,
    positionFraction: Float,
    isPlaying: Boolean,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 120.dp,
) {
    if (amplitudes.isEmpty()) return

    // Zoom: 1.0 = full file visible, higher = zoomed in
    var zoom by remember { mutableFloatStateOf(1f) }
    // Scroll offset as fraction of total width (0.0 = start)
    var scrollOffset by remember { mutableFloatStateOf(0f) }

    val waveformColor = MaterialTheme.colorScheme.primary
    val waveformBgColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
    val cursorColor = MaterialTheme.colorScheme.error
    val centerLineColor = MaterialTheme.colorScheme.outlineVariant

    // Auto-scroll: keep cursor in view during playback
    LaunchedEffect(positionFraction, isPlaying, zoom) {
        if (isPlaying && zoom > 1f) {
            val viewportWidth = 1f / zoom
            val cursorInViewport = positionFraction - scrollOffset
            if (cursorInViewport > viewportWidth * 0.8f || cursorInViewport < viewportWidth * 0.1f) {
                scrollOffset = (positionFraction - viewportWidth * 0.3f)
                    .coerceIn(0f, 1f - viewportWidth)
            }
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .pointerInput(amplitudes) {
                detectTapGestures { offset ->
                    val fraction = tapToFraction(offset.x, size.width.toFloat(), zoom, scrollOffset)
                    onSeek(fraction.coerceIn(0f, 1f))
                }
            }
            .pointerInput(amplitudes) {
                detectTransformGestures { _, _, gestureZoom, _ ->
                    val newZoom = (zoom * gestureZoom).coerceIn(1f, 50f)
                    // Adjust scroll to keep center point stable
                    val viewportCenter = scrollOffset + (1f / zoom) / 2f
                    zoom = newZoom
                    val newViewportWidth = 1f / newZoom
                    scrollOffset = (viewportCenter - newViewportWidth / 2f)
                        .coerceIn(0f, (1f - newViewportWidth).coerceAtLeast(0f))
                }
            }
            .pointerInput(amplitudes) {
                detectHorizontalDragGestures { _, dragAmount ->
                    if (zoom > 1f) {
                        val viewportWidth = 1f / zoom
                        val delta = -dragAmount / size.width * viewportWidth
                        scrollOffset = (scrollOffset + delta)
                            .coerceIn(0f, (1f - viewportWidth).coerceAtLeast(0f))
                    }
                }
            },
    ) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val centerY = canvasHeight / 2f

        // Center line
        drawLine(
            color = centerLineColor,
            start = Offset(0f, centerY),
            end = Offset(canvasWidth, centerY),
            strokeWidth = 1f,
        )

        val viewportWidth = 1f / zoom
        val startFraction = scrollOffset
        val endFraction = (scrollOffset + viewportWidth).coerceAtMost(1f)

        val startIndex = (startFraction * amplitudes.size).toInt().coerceIn(0, amplitudes.size - 1)
        val endIndex = (endFraction * amplitudes.size).toInt().coerceIn(startIndex + 1, amplitudes.size)
        val visibleCount = endIndex - startIndex

        if (visibleCount <= 0) return@Canvas

        // Draw bars
        val barWidth = canvasWidth / visibleCount
        for (i in 0 until visibleCount) {
            val amp = amplitudes[startIndex + i]
            val barHeight = amp * canvasHeight * 0.9f
            val x = i * barWidth

            // Background bar (full height indicator)
            drawRect(
                color = waveformBgColor,
                topLeft = Offset(x, centerY - canvasHeight * 0.45f),
                size = Size(barWidth.coerceAtLeast(1f), canvasHeight * 0.9f),
            )

            // Amplitude bar (mirrored around center)
            drawRect(
                color = waveformColor,
                topLeft = Offset(x, centerY - barHeight / 2f),
                size = Size(barWidth.coerceAtLeast(1f), barHeight.coerceAtLeast(1f)),
            )
        }

        // Playback cursor
        if (positionFraction in startFraction..endFraction) {
            val cursorX = ((positionFraction - startFraction) / viewportWidth) * canvasWidth
            drawLine(
                color = cursorColor,
                start = Offset(cursorX, 0f),
                end = Offset(cursorX, canvasHeight),
                strokeWidth = 2f,
            )
        }
    }
}

private fun tapToFraction(
    tapX: Float,
    canvasWidth: Float,
    zoom: Float,
    scrollOffset: Float,
): Float {
    val viewportWidth = 1f / zoom
    val tapFractionInViewport = tapX / canvasWidth
    return scrollOffset + tapFractionInViewport * viewportWidth
}

/**
 * Provides the current viewport info for the overview bar.
 */
data class WaveformViewport(
    val startFraction: Float,
    val endFraction: Float,
)
