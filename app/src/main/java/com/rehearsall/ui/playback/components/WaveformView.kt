package com.rehearsall.ui.playback.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.abs

/**
 * Interactive waveform view with pinch-to-zoom, horizontal scroll,
 * tap-to-seek, loop boundary dragging, position scrub handle, and a playback cursor.
 *
 * @param editable When true, loop boundary handles are shown and draggable.
 *   When false, loop region is shown read-only (dimmed outside, marker lines only).
 * @param showPositionHandle When true, a draggable position scrub handle appears at the bottom.
 */
@Composable
fun WaveformView(
    amplitudes: FloatArray,
    positionFraction: Float,
    isPlaying: Boolean,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 120.dp,
    zoom: Float = 1f,
    scrollOffset: Float = 0f,
    onZoomChange: (Float) -> Unit = {},
    onScrollOffsetChange: (Float) -> Unit = {},
    loopStartFraction: Float? = null,
    loopEndFraction: Float? = null,
    onLoopBoundaryDrag: ((isStart: Boolean, fraction: Float) -> Unit)? = null,
    editable: Boolean = true,
    showPositionHandle: Boolean = false,
    chunkMarkerFractions: List<Float> = emptyList(),
    activeChunkStartFraction: Float? = null,
    activeChunkEndFraction: Float? = null,
) {
    if (amplitudes.isEmpty()) return

    // Use rememberUpdatedState so gesture handlers always read latest values
    // without restarting the pointer input coroutine on every change.
    val currentZoom by rememberUpdatedState(zoom)
    val currentScroll by rememberUpdatedState(scrollOffset)
    val currentLoopStart by rememberUpdatedState(loopStartFraction)
    val currentLoopEnd by rememberUpdatedState(loopEndFraction)
    val currentPosition by rememberUpdatedState(positionFraction)

    // HIGH CONTRAST colors
    val waveformColor = MaterialTheme.colorScheme.primary
    val waveformBgColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.30f)
    val cursorColor = MaterialTheme.colorScheme.error
    val centerLineColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
    val loopOverlayColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.25f)
    val loopMarkerColor = MaterialTheme.colorScheme.tertiary
    val loopHandleFillColor = MaterialTheme.colorScheme.tertiary
    val loopHandleOutlineColor = MaterialTheme.colorScheme.onTertiary
    val chunkMarkerColor = MaterialTheme.colorScheme.secondary
    val chunkAltBgColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.15f)
    val activeChunkColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
    val dimColor = Color.Black.copy(alpha = 0.45f)
    val positionHandleColor = MaterialTheme.colorScheme.error
    val positionHandleGripColor = MaterialTheme.colorScheme.onError

    // Auto-scroll: keep cursor in view during playback
    LaunchedEffect(positionFraction, isPlaying) {
        val z = currentZoom
        val s = currentScroll
        if (isPlaying && z > 1f) {
            val vw = 1f / z
            val cursorInView = positionFraction - s
            if (cursorInView > vw * 0.8f || cursorInView < vw * 0.1f) {
                onScrollOffsetChange(
                    (positionFraction - vw * 0.3f).coerceIn(0f, 1f - vw)
                )
            }
        }
    }

    val topOverhangDp = if (editable) 16.dp else 0.dp
    val bottomOverhangDp = if (showPositionHandle) 16.dp else 0.dp

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height + topOverhangDp + bottomOverhangDp)
            .pointerInput(amplitudes) {
                // Pinch-to-zoom in its own block so two-finger gestures don't conflict
                detectTransformGestures { _, _, gestureZoom, _ ->
                    val z = currentZoom
                    val s = currentScroll
                    val newZoom = (z * gestureZoom).coerceIn(1f, 50f)
                    val viewportCenter = s + (1f / z) / 2f
                    val newVW = 1f / newZoom
                    val newScroll = (viewportCenter - newVW / 2f)
                        .coerceIn(0f, (1f - newVW).coerceAtLeast(0f))
                    onZoomChange(newZoom)
                    onScrollOffsetChange(newScroll)
                }
            }
            .pointerInput(amplitudes, editable, showPositionHandle) {
                // Single-finger: tap-to-seek, loop boundary drag, position handle drag, or scroll
                val handleHitPx = 56f
                val boundarySlop = 2f   // Very low for smooth handle scrubbing
                val generalSlop = 10f

                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val downX = down.position.x
                    val downY = down.position.y
                    val canvasW = size.width.toFloat()
                    val canvasH = size.height.toFloat()
                    val z = currentZoom
                    val s = currentScroll
                    val vw = 1f / z

                    // Check proximity to loop boundary handles (editable only)
                    var nearBoundary: Boolean? = null
                    val ls = currentLoopStart
                    val le = currentLoopEnd
                    if (editable && ls != null && le != null && onLoopBoundaryDrag != null) {
                        val startX = ((ls - s) / vw) * canvasW
                        val endX = ((le - s) / vw) * canvasW
                        val distStart = abs(downX - startX)
                        val distEnd = abs(downX - endX)
                        if (distStart < handleHitPx || distEnd < handleHitPx) {
                            nearBoundary = distStart <= distEnd
                        }
                    }

                    // Check proximity to position scrub handle
                    var nearPosition = false
                    if (showPositionHandle) {
                        val posX = ((currentPosition - s) / vw) * canvasW
                        if (abs(downX - posX) < handleHitPx) {
                            nearPosition = true
                        }
                    }

                    // Disambiguate: if both nearby, use Y (top = boundary, bottom = position)
                    if (nearBoundary != null && nearPosition) {
                        if (downY > canvasH * 0.5f) {
                            nearBoundary = null
                        } else {
                            nearPosition = false
                        }
                    }

                    val slop = if (nearBoundary != null || nearPosition) boundarySlop else generalSlop
                    var dragged = false
                    var action: String? = null

                    drag(down.id) { change ->
                        if (!dragged && abs(change.position.x - downX) > slop) {
                            dragged = true
                            action = when {
                                nearBoundary != null -> "boundary"
                                nearPosition -> "position"
                                else -> "scroll"
                            }
                        }
                        if (dragged) {
                            change.consume()
                            val cz = currentZoom
                            val cs = currentScroll
                            when (action) {
                                "boundary" -> {
                                    val f = tapToFraction(change.position.x, canvasW, cz, cs)
                                        .coerceIn(0f, 1f)
                                    onLoopBoundaryDrag?.invoke(nearBoundary!!, f)
                                }
                                "position" -> {
                                    val f = tapToFraction(change.position.x, canvasW, cz, cs)
                                        .coerceIn(0f, 1f)
                                    onSeek(f)
                                }
                                "scroll" -> {
                                    if (cz > 1f) {
                                        val dx = change.positionChange().x
                                        val cvw = 1f / cz
                                        val delta = -dx / canvasW * cvw
                                        onScrollOffsetChange(
                                            (cs + delta).coerceIn(
                                                0f,
                                                (1f - cvw).coerceAtLeast(0f),
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (!dragged) {
                        val cz = currentZoom
                        val cs = currentScroll
                        onSeek(tapToFraction(downX, canvasW, cz, cs).coerceIn(0f, 1f))
                    }
                }
            },
    ) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val topOverhangPx = if (editable) 32f else 0f
        val bottomOverhangPx = if (showPositionHandle) 32f else 0f
        val waveTop = topOverhangPx
        val waveHeight = canvasHeight - topOverhangPx - bottomOverhangPx
        val centerY = waveTop + waveHeight / 2f

        // Center line
        drawLine(
            color = centerLineColor,
            start = Offset(0f, centerY),
            end = Offset(canvasWidth, centerY),
            strokeWidth = 1f,
        )

        val viewportWidth = 1f / zoom
        val startFrac = scrollOffset
        val endFrac = (scrollOffset + viewportWidth).coerceAtMost(1f)

        // Chunk alternating backgrounds
        if (chunkMarkerFractions.isNotEmpty()) {
            val boundaries = listOf(0f) + chunkMarkerFractions.sorted() + listOf(1f)
            for (i in 0 until boundaries.size - 1) {
                val cs = boundaries[i]
                val ce = boundaries[i + 1]
                if (ce <= startFrac || cs >= endFrac) continue
                val lx = ((cs.coerceAtLeast(startFrac) - startFrac) / viewportWidth * canvasWidth)
                val rx = ((ce.coerceAtMost(endFrac) - startFrac) / viewportWidth * canvasWidth)
                if (i % 2 == 1) {
                    drawRect(
                        color = chunkAltBgColor,
                        topLeft = Offset(lx, waveTop),
                        size = Size(rx - lx, waveHeight),
                    )
                }
            }
        }

        // Active chunk highlight during practice
        if (activeChunkStartFraction != null && activeChunkEndFraction != null) {
            val lx = ((activeChunkStartFraction.coerceAtLeast(startFrac) - startFrac) / viewportWidth * canvasWidth)
            val rx = ((activeChunkEndFraction.coerceAtMost(endFrac) - startFrac) / viewportWidth * canvasWidth)
            if (rx > lx) {
                drawRect(
                    color = activeChunkColor,
                    topLeft = Offset(lx, waveTop),
                    size = Size(rx - lx, waveHeight),
                )
            }
        }

        // Draw amplitude bars
        val startIndex = (startFrac * amplitudes.size).toInt().coerceIn(0, amplitudes.size - 1)
        val endIndex = (endFrac * amplitudes.size).toInt().coerceIn(startIndex + 1, amplitudes.size)
        val visibleCount = endIndex - startIndex
        if (visibleCount <= 0) return@Canvas

        val barWidth = canvasWidth / visibleCount
        if (barWidth < 2f) {
            // Zoomed out: many samples map to fewer pixels. Render a per-pixel envelope (max
            // amplitude per pixel column) so bars don't visually merge into a solid block.
            val pixelCount = canvasWidth.toInt().coerceAtLeast(1)
            for (px in 0 until pixelCount) {
                val amp = maxAmplitudeForPixel(amplitudes, px, pixelCount, startFrac, endFrac)
                val barH = amp * waveHeight * 0.9f
                val x = px.toFloat()
                drawRect(
                    color = waveformBgColor,
                    topLeft = Offset(x, centerY - waveHeight * 0.45f),
                    size = Size(1f, waveHeight * 0.9f),
                )
                if (barH > 0f) {
                    drawRect(
                        color = waveformColor,
                        topLeft = Offset(x, centerY - barH / 2f),
                        size = Size(1f, barH.coerceAtLeast(1f)),
                    )
                }
            }
        } else {
            for (i in 0 until visibleCount) {
                val amp = amplitudes[startIndex + i]
                val barH = amp * waveHeight * 0.9f
                val x = i * barWidth

                // Background bar (visible track)
                drawRect(
                    color = waveformBgColor,
                    topLeft = Offset(x, centerY - waveHeight * 0.45f),
                    size = Size(barWidth.coerceAtLeast(1f), waveHeight * 0.9f),
                )

                // Amplitude bar (mirrored around center)
                drawRect(
                    color = waveformColor,
                    topLeft = Offset(x, centerY - barH / 2f),
                    size = Size(barWidth.coerceAtLeast(1f), barH.coerceAtLeast(1f)),
                )
            }
        }

        // Chunk marker lines
        chunkMarkerFractions.forEach { fraction ->
            if (fraction in startFrac..endFrac) {
                val mx = ((fraction - startFrac) / viewportWidth) * canvasWidth
                drawLine(
                    color = chunkMarkerColor,
                    start = Offset(mx, waveTop),
                    end = Offset(mx, waveTop + waveHeight),
                    strokeWidth = 2f,
                )
            }
        }

        // Loop region
        if (loopStartFraction != null && loopEndFraction != null && loopEndFraction > loopStartFraction) {
            val loopLX = ((loopStartFraction - startFrac) / viewportWidth * canvasWidth)
                .coerceIn(0f, canvasWidth)
            val loopRX = ((loopEndFraction - startFrac) / viewportWidth * canvasWidth)
                .coerceIn(0f, canvasWidth)

            if (editable) {
                // Semi-transparent overlay between A and B
                if (loopRX > loopLX) {
                    drawRect(
                        color = loopOverlayColor,
                        topLeft = Offset(loopLX, waveTop),
                        size = Size(loopRX - loopLX, waveHeight),
                    )
                }

                // Draggable handles: 42w x 64h, centered vertically on waveTop
                val hW = 42f
                val hH = 64f
                val hCorner = CornerRadius(6f, 6f)
                val hTop = waveTop - hH / 2f

                // A handle (start)
                if (loopStartFraction in startFrac..endFrac) {
                    drawLine(
                        color = loopMarkerColor,
                        start = Offset(loopLX, waveTop),
                        end = Offset(loopLX, waveTop + waveHeight),
                        strokeWidth = 4f,
                    )
                    drawRoundRect(
                        color = loopHandleFillColor,
                        topLeft = Offset(loopLX - 4f, hTop),
                        size = Size(hW, hH),
                        cornerRadius = hCorner,
                    )
                    val gL = loopLX + 8f
                    val gR = loopLX + hW - 14f
                    for (gy in listOf(hTop + hH * 0.3f, hTop + hH * 0.5f, hTop + hH * 0.7f)) {
                        drawLine(
                            color = loopHandleOutlineColor,
                            start = Offset(gL, gy),
                            end = Offset(gR, gy),
                            strokeWidth = 2f,
                        )
                    }
                }

                // B handle (end)
                if (loopEndFraction in startFrac..endFrac) {
                    drawLine(
                        color = loopMarkerColor,
                        start = Offset(loopRX, waveTop),
                        end = Offset(loopRX, waveTop + waveHeight),
                        strokeWidth = 4f,
                    )
                    drawRoundRect(
                        color = loopHandleFillColor,
                        topLeft = Offset(loopRX - hW + 4f, hTop),
                        size = Size(hW, hH),
                        cornerRadius = hCorner,
                    )
                    val gL = loopRX - hW + 14f
                    val gR = loopRX - 8f
                    for (gy in listOf(hTop + hH * 0.3f, hTop + hH * 0.5f, hTop + hH * 0.7f)) {
                        drawLine(
                            color = loopHandleOutlineColor,
                            start = Offset(gL, gy),
                            end = Offset(gR, gy),
                            strokeWidth = 2f,
                        )
                    }
                }
            } else {
                // Read-only: darken outside loop, show marker lines only
                if (loopLX > 0f) {
                    drawRect(
                        color = dimColor,
                        topLeft = Offset(0f, waveTop),
                        size = Size(loopLX, waveHeight),
                    )
                }
                if (loopRX < canvasWidth) {
                    drawRect(
                        color = dimColor,
                        topLeft = Offset(loopRX, waveTop),
                        size = Size(canvasWidth - loopRX, waveHeight),
                    )
                }
                if (loopStartFraction in startFrac..endFrac) {
                    drawLine(
                        color = loopMarkerColor,
                        start = Offset(loopLX, waveTop),
                        end = Offset(loopLX, waveTop + waveHeight),
                        strokeWidth = 3f,
                    )
                }
                if (loopEndFraction in startFrac..endFrac) {
                    drawLine(
                        color = loopMarkerColor,
                        start = Offset(loopRX, waveTop),
                        end = Offset(loopRX, waveTop + waveHeight),
                        strokeWidth = 3f,
                    )
                }
            }
        }

        // Position scrub handle at bottom
        if (showPositionHandle && positionFraction in startFrac..endFrac) {
            val posX = ((positionFraction - startFrac) / viewportWidth) * canvasWidth
            val phW = 42f
            val phH = 48f
            val phCorner = CornerRadius(6f, 6f)
            val phTop = waveTop + waveHeight - phH / 2f

            drawRoundRect(
                color = positionHandleColor,
                topLeft = Offset(posX - phW / 2f, phTop),
                size = Size(phW, phH),
                cornerRadius = phCorner,
            )
            val gL = posX - phW / 2f + 10f
            val gR = posX + phW / 2f - 10f
            for (gy in listOf(phTop + phH * 0.3f, phTop + phH * 0.5f, phTop + phH * 0.7f)) {
                drawLine(
                    color = positionHandleGripColor,
                    start = Offset(gL, gy),
                    end = Offset(gR, gy),
                    strokeWidth = 2f,
                )
            }
        }

        // Playback cursor line — 3 dp wide for visibility at all zoom levels
        if (positionFraction in startFrac..endFrac) {
            val cx = ((positionFraction - startFrac) / viewportWidth) * canvasWidth
            drawLine(
                color = cursorColor,
                start = Offset(cx, waveTop),
                end = Offset(cx, waveTop + waveHeight),
                strokeWidth = 3.dp.toPx(),
            )
        }
    }
}

/**
 * Returns the maximum amplitude value in [amplitudes] that maps to pixel column [px] when the
 * waveform is rendered at [pixelCount] columns wide over the fraction range [startFrac]..[endFrac].
 *
 * Used by the zoomed-out per-pixel envelope renderer to avoid multiple samples blending into
 * a solid low-contrast block.
 */
internal fun maxAmplitudeForPixel(
    amplitudes: FloatArray,
    px: Int,
    pixelCount: Int,
    startFrac: Float,
    endFrac: Float,
): Float {
    val fracStart = startFrac + (px.toFloat() / pixelCount) * (endFrac - startFrac)
    val fracEnd = startFrac + ((px + 1f) / pixelCount) * (endFrac - startFrac)
    val iStart = (fracStart * amplitudes.size).toInt().coerceIn(0, amplitudes.size - 1)
    val iEnd = (fracEnd * amplitudes.size).toInt().coerceIn(iStart, amplitudes.size - 1)
    var maxAmp = 0f
    for (i in iStart..iEnd) maxAmp = maxOf(maxAmp, amplitudes[i])
    return maxAmp
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
