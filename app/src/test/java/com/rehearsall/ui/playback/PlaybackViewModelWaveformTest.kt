package com.rehearsall.ui.playback

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.rehearsall.data.preferences.UserPreferencesRepository
import com.rehearsall.domain.model.OverlayMode
import com.rehearsall.domain.model.PracticeSettings
import com.rehearsall.playback.LoopRegion
import com.rehearsall.playback.PlaybackManager
import com.rehearsall.playback.PlaybackState
import com.rehearsall.playback.PracticeState
import com.rehearsall.playback.RepeatMode
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Verifies that [PlaybackViewModel] correctly drives contextual waveform visibility:
 * - overlayMode auto-switches to LOOPS when a loop region is set
 * - overlayMode reverts to NONE when the loop is cleared
 * - showWaveform reflects overlayMode
 * - dismissWaveform() clears the loop and hides the waveform
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PlaybackViewModelWaveformTest {
    private val testDispatcher = StandardTestDispatcher()

    private val loopRegionFlow = MutableStateFlow<LoopRegion?>(null)
    private lateinit var playbackManager: PlaybackManager
    private lateinit var userPreferencesRepository: UserPreferencesRepository

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        playbackManager =
            mockk(relaxed = true) {
                every { playbackState } returns MutableStateFlow(PlaybackState.IDLE)
                every { currentFileId } returns MutableStateFlow(1L)
                every { repeatMode } returns MutableStateFlow(RepeatMode.OFF)
                every { shuffleEnabled } returns MutableStateFlow(false)
                every { currentQueue } returns MutableStateFlow(emptyList())
                every { loopRegion } returns loopRegionFlow
            }

        userPreferencesRepository =
            mockk(relaxed = true) {
                every { skipIncrementMs } returns MutableStateFlow(5000L)
                every { waveformOverlay } returns MutableStateFlow(OverlayMode.NONE)
            }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): PlaybackViewModel {
        return PlaybackViewModel(
            savedStateHandle = SavedStateHandle(mapOf("audioFileId" to 1L)),
            playbackManager = playbackManager,
            repository = mockk(relaxed = true) { coEvery { getById(any()) } returns null },
            waveformRepository = mockk(relaxed = true) { every { getWaveform(any(), any()) } returns flowOf() },
            bookmarkRepository = mockk(relaxed = true) { every { getBookmarksForFile(any()) } returns flowOf(emptyList()) },
            loopRepository = mockk(relaxed = true) { every { getLoopsForFile(any()) } returns flowOf(emptyList()) },
            chunkMarkerRepository = mockk(relaxed = true) { every { getMarkersForFile(any()) } returns flowOf(emptyList()) },
            practiceSettingsRepository = mockk(relaxed = true) { coEvery { getForFile(any()) } returns PracticeSettings() },
            practiceEngine = mockk(relaxed = true) { every { state } returns MutableStateFlow(PracticeState.Idle) },
            userPreferencesRepository = userPreferencesRepository,
        )
    }

    @Test
    fun `overlayMode becomes LOOPS when a loop region is set`() =
        runTest {
            val viewModel = createViewModel()
            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.uiState.test {
                awaitItem() // consume initial state

                loopRegionFlow.value = LoopRegion(1000L, 5000L)
                testDispatcher.scheduler.advanceUntilIdle()

                val state = awaitItem()
                assertEquals(OverlayMode.LOOPS, state.overlayMode)
            }
        }

    @Test
    fun `overlayMode reverts to NONE when loop is cleared after being LOOPS`() =
        runTest {
            val viewModel = createViewModel()
            loopRegionFlow.value = LoopRegion(1000L, 5000L)
            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.uiState.test {
                awaitItem() // current state (LOOPS)

                loopRegionFlow.value = null
                testDispatcher.scheduler.advanceUntilIdle()

                val state = awaitItem()
                assertEquals(OverlayMode.NONE, state.overlayMode)
            }
        }

    @Test
    fun `showWaveform is true when overlayMode is LOOPS`() =
        runTest {
            val viewModel = createViewModel()
            testDispatcher.scheduler.advanceUntilIdle()

            loopRegionFlow.value = LoopRegion(1000L, 5000L)
            testDispatcher.scheduler.advanceUntilIdle()

            assertTrue(viewModel.uiState.value.showWaveform)
        }

    @Test
    fun `showWaveform is false when overlayMode is NONE`() =
        runTest {
            val viewModel = createViewModel()
            testDispatcher.scheduler.advanceUntilIdle()

            assertFalse(viewModel.uiState.value.showWaveform)
        }

    @Test
    fun `dismissWaveform clears the loop and sets overlayMode to NONE`() =
        runTest {
            val viewModel = createViewModel()
            loopRegionFlow.value = LoopRegion(1000L, 5000L)
            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.dismissWaveform()
            testDispatcher.scheduler.advanceUntilIdle()

            verify { playbackManager.clearLoopRegion() }
            assertEquals(OverlayMode.NONE, viewModel.uiState.value.overlayMode)
            assertFalse(viewModel.uiState.value.showWaveform)
        }
}
