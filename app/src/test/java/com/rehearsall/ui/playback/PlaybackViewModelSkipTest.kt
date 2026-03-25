package com.rehearsall.ui.playback

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.rehearsall.data.preferences.UserPreferencesRepository
import com.rehearsall.data.repository.AudioFileRepository
import com.rehearsall.data.repository.BookmarkRepository
import com.rehearsall.data.repository.ChunkMarkerRepository
import com.rehearsall.data.repository.LoopRepository
import com.rehearsall.data.repository.PracticeSettingsRepository
import com.rehearsall.data.repository.WaveformRepository
import com.rehearsall.domain.model.OverlayMode
import com.rehearsall.domain.model.PracticeSettings
import com.rehearsall.playback.ChunkedPracticeEngine
import com.rehearsall.playback.PlaybackManager
import com.rehearsall.playback.PlaybackState
import com.rehearsall.playback.PracticeState
import com.rehearsall.playback.RepeatMode
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * Verifies that [PlaybackViewModel] emits navigation events when [PlaybackManager.currentFileId]
 * transitions to a different track — the mechanism that makes skip-to-next/previous work correctly
 * when a queue is active.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PlaybackViewModelSkipTest {
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var playbackManager: PlaybackManager
    private val currentFileIdFlow = MutableStateFlow<Long?>(1L)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        playbackManager =
            mockk(relaxed = true) {
                every { playbackState } returns MutableStateFlow(PlaybackState.IDLE)
                every { currentFileId } returns currentFileIdFlow
                every { repeatMode } returns MutableStateFlow(RepeatMode.OFF)
                every { shuffleEnabled } returns MutableStateFlow(false)
                every { currentQueue } returns MutableStateFlow(emptyList())
                every { loopRegion } returns MutableStateFlow(null)
            }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(fileId: Long = 1L): PlaybackViewModel {
        val audioFileRepository =
            mockk<AudioFileRepository>(relaxed = true) {
                coEvery { getById(any()) } returns null
            }
        val waveformRepository =
            mockk<WaveformRepository>(relaxed = true) {
                every { getWaveform(any(), any()) } returns flowOf()
            }
        val bookmarkRepository =
            mockk<BookmarkRepository>(relaxed = true) {
                every { getBookmarksForFile(any()) } returns flowOf(emptyList())
            }
        val loopRepository =
            mockk<LoopRepository>(relaxed = true) {
                every { getLoopsForFile(any()) } returns flowOf(emptyList())
            }
        val chunkMarkerRepository =
            mockk<ChunkMarkerRepository>(relaxed = true) {
                every { getMarkersForFile(any()) } returns flowOf(emptyList())
            }
        val practiceSettingsRepository =
            mockk<PracticeSettingsRepository>(relaxed = true) {
                coEvery { getForFile(any()) } returns PracticeSettings()
            }
        val practiceEngine =
            mockk<ChunkedPracticeEngine>(relaxed = true) {
                every { state } returns MutableStateFlow(PracticeState.Idle)
            }
        val userPreferencesRepository =
            mockk<UserPreferencesRepository>(relaxed = true) {
                every { skipIncrementMs } returns MutableStateFlow(5000L)
                every { waveformOverlay } returns MutableStateFlow(OverlayMode.NONE)
            }

        return PlaybackViewModel(
            savedStateHandle = SavedStateHandle(mapOf("audioFileId" to fileId)),
            playbackManager = playbackManager,
            repository = audioFileRepository,
            waveformRepository = waveformRepository,
            bookmarkRepository = bookmarkRepository,
            loopRepository = loopRepository,
            chunkMarkerRepository = chunkMarkerRepository,
            practiceSettingsRepository = practiceSettingsRepository,
            practiceEngine = practiceEngine,
            userPreferencesRepository = userPreferencesRepository,
        )
    }

    @Test
    fun `navigationEvent emits new file ID when currentFileId transitions to a different track`() =
        runTest {
            val viewModel = createViewModel(fileId = 1L)
            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.navigationEvent.test {
                currentFileIdFlow.value = 2L
                testDispatcher.scheduler.advanceUntilIdle()
                assert(awaitItem() == 2L)
            }
        }

    @Test
    fun `navigationEvent does not emit when currentFileId stays on the same file`() =
        runTest {
            val viewModel = createViewModel(fileId = 1L)
            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.navigationEvent.test {
                currentFileIdFlow.value = 1L
                testDispatcher.scheduler.advanceUntilIdle()
                expectNoEvents()
            }
        }

    @Test
    fun `navigationEvent does not emit when currentFileId becomes null`() =
        runTest {
            val viewModel = createViewModel(fileId = 1L)
            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.navigationEvent.test {
                currentFileIdFlow.value = null
                testDispatcher.scheduler.advanceUntilIdle()
                expectNoEvents()
            }
        }
}
