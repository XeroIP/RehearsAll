package com.rehearsall.ui.filelist

import android.net.Uri
import app.cash.turbine.test
import com.rehearsall.data.audio.AudioImporter
import com.rehearsall.data.repository.AudioFileRepository
import com.rehearsall.data.repository.PlaylistRepository
import com.rehearsall.domain.model.AudioFile
import com.rehearsall.domain.model.Playlist
import com.rehearsall.playback.PlaybackManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

@OptIn(ExperimentalCoroutinesApi::class)
class FileListViewModelSelectionTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var repository: AudioFileRepository
    private lateinit var playlistRepository: PlaylistRepository
    private lateinit var viewModel: FileListViewModel

    private val file1 = AudioFile(
        id = 1L,
        displayName = "Track One",
        internalPath = "/data/track1.mp3",
        durationMs = 60_000L,
        format = "mp3",
        artist = null,
    )
    private val file2 = AudioFile(
        id = 2L,
        displayName = "Track Two",
        internalPath = "/data/track2.mp3",
        durationMs = 90_000L,
        format = "mp3",
        artist = null,
    )
    private val playlist = Playlist(id = 10L, name = "Favorites", trackCount = 0, totalDurationMs = 0L)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        repository = mockk(relaxed = true) {
            every { getAllFiles() } returns flowOf(listOf(file1, file2))
        }
        playlistRepository = mockk(relaxed = true) {
            every { getAllPlaylists() } returns flowOf(listOf(playlist))
            coEvery { getById(playlist.id) } returns playlist
        }

        viewModel = FileListViewModel(
            repository = repository,
            playlistRepository = playlistRepository,
            importer = mockk(relaxed = true),
            playbackManager = mockk(relaxed = true),
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun advanceToLoaded() {
        testDispatcher.scheduler.advanceUntilIdle()
    }

    // --- toggleFileSelection ---

    @Test
    fun `toggleFileSelection adds id to selectedFileIds`() = runTest {
        advanceToLoaded()

        viewModel.toggleFileSelection(file1.id)

        val state = viewModel.uiState.value as FileListUiState.Loaded
        assertTrue(file1.id in state.selectedFileIds)
    }

    @Test
    fun `toggleFileSelection removes id when already selected`() = runTest {
        advanceToLoaded()
        viewModel.toggleFileSelection(file1.id)

        viewModel.toggleFileSelection(file1.id)

        val state = viewModel.uiState.value as FileListUiState.Loaded
        assertFalse(file1.id in state.selectedFileIds)
    }

    @Test
    fun `toggleFileSelection can select multiple ids independently`() = runTest {
        advanceToLoaded()

        viewModel.toggleFileSelection(file1.id)
        viewModel.toggleFileSelection(file2.id)

        val state = viewModel.uiState.value as FileListUiState.Loaded
        assertTrue(file1.id in state.selectedFileIds)
        assertTrue(file2.id in state.selectedFileIds)
    }

    @Test
    fun `isInSelectionMode is true when any file is selected`() = runTest {
        advanceToLoaded()

        viewModel.toggleFileSelection(file1.id)

        val state = viewModel.uiState.value as FileListUiState.Loaded
        assertTrue(state.isInSelectionMode)
    }

    @Test
    fun `isInSelectionMode is false when no files are selected`() = runTest {
        advanceToLoaded()

        val state = viewModel.uiState.value as FileListUiState.Loaded
        assertFalse(state.isInSelectionMode)
    }

    // --- clearSelection ---

    @Test
    fun `clearSelection empties selectedFileIds`() = runTest {
        advanceToLoaded()
        viewModel.toggleFileSelection(file1.id)
        viewModel.toggleFileSelection(file2.id)

        viewModel.clearSelection()

        val state = viewModel.uiState.value as FileListUiState.Loaded
        assertTrue(state.selectedFileIds.isEmpty())
    }

    @Test
    fun `clearSelection on empty selection does not crash`() = runTest {
        advanceToLoaded()

        viewModel.clearSelection() // no-op, should not throw

        val state = viewModel.uiState.value as FileListUiState.Loaded
        assertTrue(state.selectedFileIds.isEmpty())
    }

    // --- addSelectedToPlaylist ---

    @Test
    fun `addSelectedToPlaylist calls addFileToPlaylist for each selected id`() = runTest {
        advanceToLoaded()
        viewModel.toggleFileSelection(file1.id)
        viewModel.toggleFileSelection(file2.id)

        viewModel.addSelectedToPlaylist(playlist.id)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { playlistRepository.addFileToPlaylist(playlist.id, file1.id) }
        coVerify { playlistRepository.addFileToPlaylist(playlist.id, file2.id) }
    }

    @Test
    fun `addSelectedToPlaylist clears selection after adding`() = runTest {
        advanceToLoaded()
        viewModel.toggleFileSelection(file1.id)

        viewModel.addSelectedToPlaylist(playlist.id)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value as FileListUiState.Loaded
        assertTrue(state.selectedFileIds.isEmpty())
    }

    @Test
    fun `addSelectedToPlaylist emits AddedBatchToPlaylist event`() = runTest {
        advanceToLoaded()
        viewModel.toggleFileSelection(file1.id)
        viewModel.toggleFileSelection(file2.id)

        viewModel.events.test {
            viewModel.addSelectedToPlaylist(playlist.id)
            testDispatcher.scheduler.advanceUntilIdle()

            val event = awaitItem() as FileListEvent.AddedBatchToPlaylist
            assertEquals(2, event.count)
            assertEquals(playlist.name, event.playlistName)
        }
    }

    @Test
    fun `addSelectedToPlaylist does nothing when selection is empty`() = runTest {
        advanceToLoaded()

        viewModel.addSelectedToPlaylist(playlist.id)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 0) { playlistRepository.addFileToPlaylist(any(), any()) }
    }
}
