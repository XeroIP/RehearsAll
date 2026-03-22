package com.rehearsall.ui.playback.components

import org.junit.Assert.assertEquals
import org.junit.Test

class WaveformViewTest {

    private val tolerance = 0.001f

    @Test
    fun `maxAmplitudeForPixel returns max amplitude across all samples for a single pixel`() {
        val amplitudes = floatArrayOf(0.1f, 0.5f, 0.3f, 0.9f, 0.2f)
        // Pixel 0 covers the full range (startFrac=0, endFrac=1, pixelCount=1)
        val result = maxAmplitudeForPixel(amplitudes, px = 0, pixelCount = 1, startFrac = 0f, endFrac = 1f)
        assertEquals(0.9f, result, tolerance)
    }

    @Test
    fun `maxAmplitudeForPixel maps each pixel to correct sample range`() {
        // 4 amplitudes, 4 pixels, full range — each pixel maps to exactly one sample
        val amplitudes = floatArrayOf(0.2f, 0.8f, 0.4f, 0.6f)
        assertEquals(0.2f, maxAmplitudeForPixel(amplitudes, 0, 4, 0f, 1f), tolerance)
        assertEquals(0.8f, maxAmplitudeForPixel(amplitudes, 1, 4, 0f, 1f), tolerance)
        assertEquals(0.4f, maxAmplitudeForPixel(amplitudes, 2, 4, 0f, 1f), tolerance)
        assertEquals(0.6f, maxAmplitudeForPixel(amplitudes, 3, 4, 0f, 1f), tolerance)
    }

    @Test
    fun `maxAmplitudeForPixel handles zoomed viewport (startFrac to endFrac subset)`() {
        // 10 samples; view covers only the middle 50% (samples 5-9)
        val amplitudes = floatArrayOf(0.1f, 0.1f, 0.1f, 0.1f, 0.1f, 0.3f, 0.7f, 0.5f, 0.9f, 0.2f)
        // With pixelCount=5, each pixel maps to one sample in the 0.5..1.0 range
        val result = maxAmplitudeForPixel(amplitudes, px = 3, pixelCount = 5, startFrac = 0.5f, endFrac = 1.0f)
        // Pixel 3 in a 5-pixel view of 0.5..1.0 maps to sample index 8 (amplitude 0.9)
        assertEquals(0.9f, result, tolerance)
    }

    @Test
    fun `maxAmplitudeForPixel returns 0 for silent audio`() {
        val amplitudes = floatArrayOf(0f, 0f, 0f, 0f)
        val result = maxAmplitudeForPixel(amplitudes, px = 0, pixelCount = 4, startFrac = 0f, endFrac = 1f)
        assertEquals(0f, result, tolerance)
    }

    @Test
    fun `maxAmplitudeForPixel is stable at boundary pixels`() {
        val amplitudes = floatArrayOf(0.5f, 0.6f, 0.7f, 0.8f)
        // First and last pixel should not throw or return garbage
        val first = maxAmplitudeForPixel(amplitudes, px = 0, pixelCount = 100, startFrac = 0f, endFrac = 1f)
        val last = maxAmplitudeForPixel(amplitudes, px = 99, pixelCount = 100, startFrac = 0f, endFrac = 1f)
        assert(first >= 0f) { "First pixel amplitude should be non-negative" }
        assert(last >= 0f) { "Last pixel amplitude should be non-negative" }
    }
}
