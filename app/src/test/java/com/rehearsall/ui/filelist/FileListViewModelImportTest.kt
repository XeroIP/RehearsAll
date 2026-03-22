package com.rehearsall.ui.filelist

import android.net.Uri
import app.cash.turbine.test
import com.rehearsall.data.audio.AudioImporter
import com.rehearsall.data.repository.AudioFileRepository
import com.rehearsall.data.repository.PlaylistRepository
import com.rehearsall.domain.model.AudioFile
import java.time.Instant
import io.mockk.coEvery
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
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FileListViewModelImportTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var importer: AudioImporter
    private lateinit var viewModel: FileListViewModel

    private val fakeFile = AudioFile(
        id = 1L,
        displayName = "Track One",
        artist = null,
        title = null,
        format = "mp3",
        durationMs = 60_000L,
        fileSizeBytes = 0L,
        internalPath = "/data/track1.mp3",
        importedAt = Instant.EPOCH,
        lastPlayedAt = null,
        lastPositionMs = 0L,
        lastSpeed = 1f,
    )

    private val uri1 = mockk<Uri>(relaxed = true)
    private val uri2 = mockk<Uri>(relaxed = true)
    private val uri3 = mockk<Uri>(relaxed = true)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        importer = mockk(relaxed = true)

        val repository = mockk<AudioFileRepository>(relaxed = true) {
            every { getAllFiles() } returns flowOf(emptyList())
        }
        val playlistRepository = mockk<PlaylistRepository>(relaxed = true) {
            every { getAllPlaylists() } returns flowOf(emptyList())
        }

        viewModel = FileListViewModel(
            repository = repository,
            playlistRepository = playlistRepository,
            importer = importer,
            playbackManager = mockk(relaxed = true),
        )

        testDispatcher.scheduler.advanceUntilIdle()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `importFiles with empty list does nothing`() = runTest {
        viewModel.events.test {
            viewModel.importFiles(emptyList())
            testDispatcher.scheduler.advanceUntilIdle()
            expectNoEvents()
        }
    }

    @Test
    fun `importFiles with single uri delegates to importFile and emits ImportSuccess`() = runTest {
        coEvery { importer.import(uri1) } returns Result.success(fakeFile)

        viewModel.events.test {
            viewModel.importFiles(listOf(uri1))
            testDispatcher.scheduler.advanceUntilIdle()

            val event = awaitItem() as FileListEvent.ImportSuccess
            assertEquals(fakeFile.displayName, event.displayName)
        }
    }

    @Test
    fun `importFiles with multiple uris emits ImportBatchComplete on all success`() = runTest {
        coEvery { importer.import(uri1) } returns Result.success(fakeFile)
        coEvery { importer.import(uri2) } returns Result.success(fakeFile.copy(id = 2L, displayName = "Track Two"))
        coEvery { importer.import(uri3) } returns Result.success(fakeFile.copy(id = 3L, displayName = "Track Three"))

        viewModel.events.test {
            viewModel.importFiles(listOf(uri1, uri2, uri3))
            testDispatcher.scheduler.advanceUntilIdle()

            val event = awaitItem() as FileListEvent.ImportBatchComplete
            assertEquals(3, event.count)
        }
    }

    @Test
    fun `importFiles emits ImportError when some uris fail`() = runTest {
        coEvery { importer.import(uri1) } returns Result.success(fakeFile)
        coEvery { importer.import(uri2) } returns Result.failure(Exception("bad file"))

        viewModel.events.test {
            viewModel.importFiles(listOf(uri1, uri2))
            testDispatcher.scheduler.advanceUntilIdle()

            val batch = awaitItem() as FileListEvent.ImportBatchComplete
            assertEquals(1, batch.count)

            val error = awaitItem() as FileListEvent.ImportError
            assertTrue(error.message.contains("1"))
        }
    }

    @Test
    fun `isImporting is false after batch import completes`() = runTest {
        coEvery { importer.import(uri1) } returns Result.success(fakeFile)
        coEvery { importer.import(uri2) } returns Result.success(fakeFile.copy(id = 2L))

        viewModel.importFiles(listOf(uri1, uri2))
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.isImporting.value)
    }
}
