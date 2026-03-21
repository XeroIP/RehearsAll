package com.rehearsall.ui.playback.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

/**
 * Minimap showing the full waveform with a highlighted rectangle
 * indicating the currently visible viewport in the main WaveformView.
 *
 * Supports tap and drag to reposition the viewport.
 */
@Composable
fun WaveformOverviewBar(
    amplitudes: FloatArray,
    positionFraction: Float,
    viewportStart: Float,
    viewportEnd: Float,
    modifier: Modifier = Modifier,
    onViewportDrag: ((newScrollOffset: Float) -> Unit)? = null,
) {
    if (amplitudes.isEmpty()) return

    val barColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
    val viewportColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)
    val viewportBorderColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.7f)
    val cursorColor = MaterialTheme.colorScheme.error

    val currentVpStart by rememberUpdatedState(viewportStart)
    val currentVpEnd by rememberUpdatedState(viewportEnd)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(24.dp)
            .then(
                if (onViewportDrag != null) {
                    Modifier.pointerInput(Unit) {
                        val vpWidth = { currentVpEnd - currentVpStart }
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val canvasW = size.width.toFloat()
                            // Center viewport on tap position
                            val fraction = (down.position.x / canvasW).coerceIn(0f, 1f)
                            val halfVp = vpWidth() / 2f
                            onViewportDrag(
                                (fraction - halfVp).coerceIn(0f, (1f - vpWidth()).coerceAtLeast(0f))
                            )

                            drag(down.id) { change ->
                                change.consume()
                                val f = (change.position.x / canvasW).coerceIn(0f, 1f)
                                val hw = vpWidth() / 2f
                                onViewportDrag(
                                    (f - hw).coerceIn(0f, (1f - vpWidth()).coerceAtLeast(0f))
                                )
                            }
                        }
                    }
                } else {
                    Modifier
                }
            ),
    ) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val centerY = canvasHeight / 2f

        // Draw mini waveform
        val step = (amplitudes.size / canvasWidth).toInt().coerceAtLeast(1)
        var x = 0f
        val barWidth = canvasWidth / (amplitudes.size.toFloat() / step)

        var i = 0
        while (i < amplitudes.size) {
            // Take max amplitude in this pixel group
            var maxAmp = 0f
            for (j in i until (i + step).coerceAtMost(amplitudes.size)) {
                if (amplitudes[j] > maxAmp) maxAmp = amplitudes[j]
            }

            val barHeight = maxAmp * canvasHeight * 0.8f
            drawRect(
                color = barColor,
                topLeft = Offset(x, centerY - barHeight / 2f),
                size = Size(barWidth.coerceAtLeast(1f), barHeight.coerceAtLeast(0.5f)),
            )

            x += barWidth
            i += step
        }

        // Viewport highlight rectangle
        val vpStartX = viewportStart * canvasWidth
        val vpEndX = viewportEnd * canvasWidth
        val vpWidth = (vpEndX - vpStartX).coerceAtLeast(2f)

        drawRect(
            color = viewportColor,
            topLeft = Offset(vpStartX, 0f),
            size = Size(vpWidth, canvasHeight),
        )

        // Viewport borders
        drawLine(
            color = viewportBorderColor,
            start = Offset(vpStartX, 0f),
            end = Offset(vpStartX, canvasHeight),
            strokeWidth = 1.5f,
        )
        drawLine(
            color = viewportBorderColor,
            start = Offset(vpStartX + vpWidth, 0f),
            end = Offset(vpStartX + vpWidth, canvasHeight),
            strokeWidth = 1.5f,
        )

        // Position cursor
        val cursorX = positionFraction * canvasWidth
        drawLine(
            color = cursorColor,
            start = Offset(cursorX, 0f),
            end = Offset(cursorX, canvasHeight),
            strokeWidth = 1.5f,
        )
    }
}
